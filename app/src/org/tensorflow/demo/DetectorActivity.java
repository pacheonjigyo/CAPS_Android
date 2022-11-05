package org.tensorflow.demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.base.CharMatcher;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import org.tensorflow.demo.OverlayView.DrawCallback;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.tracking.MultiBoxTracker;

import static android.content.ContentValues.TAG;

public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    LoginActivity activity = (LoginActivity) LoginActivity.loginActivity;

    private static final int YOLO_INPUT_SIZE = 416;
    private static final int YOLO_BLOCK_SIZE = 32;

    private static final String YOLO_MODEL_FILE = "file:///android_asset/caps.pb";

    private static final String YOLO_INPUT_NAME = "input";
    private static final String YOLO_OUTPUT_NAMES = "output";

    private enum DetectorMode {YOLO;}

    private static final DetectorMode MODE = DetectorMode.YOLO;

    private static final float MINIMUM_CONFIDENCE_YOLO = 0.25f;

    private static final boolean MAINTAIN_ASPECT = MODE == DetectorMode.YOLO;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;

    private Classifier detector;

    private long lastProcessingTimeMs;

    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private byte[] luminanceCopy;

    private BorderedText borderedText;

    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private StorageReference mountainReference;
    private DatabaseReference ocrReference;

    private String previousNumber;
    private String plateNumber;
    private boolean isfirstTime = true;
    private boolean isOneTouch = true;
    private TextView textView;
    private int testNum=0;

    boolean valve = false;

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        databaseReference = FirebaseDatabase.getInstance().getReference("id_list");
        ocrReference = FirebaseDatabase.getInstance().getReference("ocr");

        storageReference = FirebaseStorage.getInstance().getReference();
        textView = (TextView) findViewById(R.id.textView3);
        Spannable span = (Spannable) textView.getText();
        span.setSpan(new ForegroundColorSpan(Color.rgb(0,230,172)), 17, 20, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new StyleSpan(Typeface.BOLD), 17, 20, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        final float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());

        final Button btn_update = (Button) findViewById(R.id.update);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = getIntent();
                String name = intent2.getExtras().getString("id");
                Intent intent = new Intent(getApplicationContext(), PopUpActivity.class);
                intent.putExtra("id", name);
                startActivity(intent);
            }
        });

        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = YOLO_INPUT_SIZE;

        detector = TensorFlowYoloDetector.create(getAssets(), YOLO_MODEL_FILE, YOLO_INPUT_SIZE, YOLO_INPUT_NAME, YOLO_OUTPUT_NAMES, YOLO_BLOCK_SIZE);

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(previewWidth, previewHeight, cropSize, cropSize, sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (!isDebug()) {
                            return;
                        }
                        final Bitmap copy = cropCopyBitmap;
                        if (copy == null) {
                            return;
                        }

                        final int backgroundColor = Color.argb(100, 0, 0, 0);
                        canvas.drawColor(backgroundColor);

                        final Matrix matrix = new Matrix();
                        final float scaleFactor = 2;
                        matrix.postScale(scaleFactor, scaleFactor);
                        matrix.postTranslate(
                                canvas.getWidth() - copy.getWidth() * scaleFactor,
                                canvas.getHeight() - copy.getHeight() * scaleFactor);
                        canvas.drawBitmap(copy, matrix, new Paint());

                        final Vector<String> lines = new Vector<String>();
                        if (detector != null) {
                            final String statString = detector.getStatString();
                            final String[] statLines = statString.split("\n");
                            for (final String line : statLines) {
                                lines.add(line);
                            }
                        }
                    }
                });
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
    }

    OverlayView trackingOverlay;

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        byte[] originalLuminance = getLuminance();
        tracker.onFrame(
                previewWidth,
                previewHeight,
                getLuminanceStride(),
                sensorOrientation,
                originalLuminance,
                timestamp);
        trackingOverlay.postInvalidate();

        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }

        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(new Runnable() {
            @Override
            public void run() {
                final long startTime = SystemClock.uptimeMillis();
                final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final Canvas canvas = new Canvas(cropCopyBitmap);
                final Paint paint = new Paint();
                paint.setColor(Color.YELLOW);
                paint.setStyle(Style.STROKE);
                paint.setStrokeWidth(2.0f);

                float minimumConfidence = MINIMUM_CONFIDENCE_YOLO;

                final List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();

                for (final Classifier.Recognition result : results) {
                    final RectF location = result.getLocation();

                    if (location != null && result.getConfidence() >= minimumConfidence) {
                        canvas.drawRect(location, paint);

                        cropToFrameTransform.mapRect(location);
                        result.setLocation(location);
                        mappedRecognitions.add(result);
                    }
                }

                tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
                trackingOverlay.postInvalidate();

                requestRender();
                computingDetection = false;
            }
        });
    }

    public String removeSymbol(String result){
        final StringBuffer platenumber = new StringBuffer();
        final StringBuffer output = new StringBuffer();
        final String match = "[^\uAC00-\uD7A30-9\\s]";
        String test = new String();
        test = result.replaceAll(match, "");

        final String[] array1 = test.split("\n");

        for(int i = 0; i < array1.length; i++) {
            if (array1[i] != null)
                output.append(array1[i]);
        }

        final String[] array2 = output.toString().split(" ");

        for(int i = 0; i < array2.length; i++) {
            if (array2[i] != null)
                platenumber.append(array2[i]);
        }

        return platenumber.toString();
    }

    long first_time = System.currentTimeMillis();

    @Override
    public void onBackPressed() {

        long second_time = System.currentTimeMillis();
        Toast.makeText(this, "한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
        if(second_time - first_time < 2000){
            super.onBackPressed();
            activity.finish();
            finishAffinity();
        }
        first_time = System.currentTimeMillis();
    }

    long first_time2 = System.currentTimeMillis();

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            long second_time = System.currentTimeMillis();
            if(second_time - first_time2 <= 2500){
                return false;
            }
            first_time2 = System.currentTimeMillis();
            float x = event.getX();
            float y = event.getY();

            if (tracker.getBox() != null && tracker.getBox().contains(x, y)) {

                int croppedx = (int) ((tracker.getBox().left / 2) - 20);
                int croppedy = (int) ((tracker.getBox().top / 2) - 50);
                int croppedw = (int) ((tracker.getBox().width() / 2));
                int croppedh = (int) ((tracker.getBox().height() / 2));

                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(90);

                Bitmap sideInversionImg = Bitmap.createBitmap(rgbFrameBitmap, 0, 0, rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), rotateMatrix, true);

                if(croppedx + croppedw > 0 && croppedx + croppedw <= sideInversionImg.getWidth() && croppedy + croppedh > 0 && croppedy + croppedh <= sideInversionImg.getHeight()) {
                    Bitmap croppedBmp = Bitmap.createBitmap(sideInversionImg, croppedx, croppedy, croppedw, croppedh);
                    mountainReference = storageReference.child("image/test.jpg");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    croppedBmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] data = baos.toByteArray();
                    UploadTask uploadTask = mountainReference.putBytes(data);
                    Log.i(TAG, "onTouchEvent: "+testNum++);
                    Toast.makeText(getApplication(), "검출된 영역을 분석합니다.", Toast.LENGTH_SHORT).show();
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            try {
                                Thread.sleep(3000);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        ocrReference.orderByValue().addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(isfirstTime) {
                                                    plateNumber = removeSymbol(dataSnapshot.getValue().toString());
                                                    displayDialog(plateNumber);
                                                    isfirstTime=false;
                                                }else {
                                                    isfirstTime=true;
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                            }
                                        });
                                    }
                                }, 1);
                            }catch (InterruptedException e){
                                e.printStackTrace();
                            }
                        }
                    });
                }
                else
                    Toast.makeText(getApplication(), "다시 시도해주세요.", Toast.LENGTH_SHORT).show();

                return true;
            }
        }

        return false;
    }

    public void displayDialog(final String output){
        final Dialog platedialog = new Dialog(this);
        final Dialog resultdialog = new Dialog(this);
        final Dialog errordialog = new Dialog(this);
        final Dialog searchdialog = new Dialog(this);

        platedialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        platedialog.setContentView(R.layout.dialog_plate);
        platedialog.show();

        final Button plate = (Button) platedialog.findViewById(R.id.button);
        final Button okButton = (Button) platedialog.findViewById(R.id.okButton);
        final Button cancelButton = (Button) platedialog.findViewById(R.id.cancelButton);

        plate.setText(output);

        valve = false;

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                platedialog.dismiss();

                searchdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                searchdialog.setContentView(R.layout.dialog_search);
                searchdialog.show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!valve) {
                            Log.i(TAG, "myrun: -1");
                            searchdialog.dismiss();

                            errordialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            errordialog.setContentView(R.layout.dialog_error);
                            errordialog.show();

                            final Button okButton_error = (Button) errordialog.findViewById(R.id.okButton);
                            okButton_error.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    errordialog.dismiss();
                                }
                            });
                            Log.i(TAG, "myrun: 0");
                        }
                    }
                }, 1500);
                Log.i(TAG, "onClick: 1");

                databaseReference.orderByChild("plate").addChildEventListener(new ChildEventListener() {

                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                        FirebasePost user = dataSnapshot.getValue(FirebasePost.class);

                        if (user.plate.equals(output)) {
                            searchdialog.dismiss();

                            resultdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            resultdialog.setContentView(R.layout.dialog_result);
                            resultdialog.show();

                            final TextView textView6_result = (TextView) resultdialog.findViewById(R.id.textView6);
                            final TextView textView7_result = (TextView) resultdialog.findViewById(R.id.textView7);
                            final TextView textView8_result = (TextView) resultdialog.findViewById(R.id.textView8);
                            final TextView textView9_result = (TextView) resultdialog.findViewById(R.id.textView9);
                            final TextView textView0_result = (TextView) resultdialog.findViewById(R.id.textView10);

                            final Button okButton_result = (Button) resultdialog.findViewById(R.id.okButton);

                            textView6_result.setText(user.plate + "(" + user.car + ")");
                            textView7_result.setText(user.name + "(" + user.gender + ")");
                            textView8_result.setText(user.phone);
                            textView9_result.setText(user.address);
                            textView0_result.setText(user.email);
                            Log.i(TAG, "onChildAdded: 2");
                            okButton_result.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    resultdialog.dismiss();
                                }
                            });

                            valve = true;
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
        });
        Log.i(TAG, "displayDialog: 3");
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                platedialog.dismiss();
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }
}