package org.tensorflow.demo.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.Pair;
import android.util.TypedValue;
import android.widget.Toast;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.tensorflow.demo.Classifier.Recognition;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;

public class MultiBoxTracker {

  private static final float TEXT_SIZE_DIP = 18;
  private static final float MAX_OVERLAP = 0.2f;
  private static final float MIN_SIZE = 16.0f;
  private static final float MARGINAL_CORRELATION = 0.75f;
  private static final float MIN_CORRELATION = 0.3f;
  private static final int[] COLORS = {Color.rgb(0, 230, 172)};
  private final Queue<Integer> availableColors = new LinkedList<Integer>();
  public ObjectTracker objectTracker;

  final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();

  private static class TrackedRecognition {
    ObjectTracker.TrackedObject trackedObject;
    RectF location;
    float detectionConfidence;
    int color;
    String title;
  }

  private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();

  private final Paint boxPaint = new Paint();

  private final float textSizePx;
  private final BorderedText borderedText;

  private Matrix frameToCanvasMatrix;

  private int frameWidth;
  private int frameHeight;

  private int sensorOrientation;
  private Context context;

  private RectF location;

  public MultiBoxTracker(final Context context) {
    this.context = context;
    for (final int color : COLORS) {
      availableColors.add(color);
    }

    boxPaint.setColor(Color.rgb(0, 230, 172));
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(12.0f);
    boxPaint.setStrokeCap(Cap.ROUND);
    boxPaint.setStrokeJoin(Join.ROUND);
    boxPaint.setStrokeMiter(100);

    textSizePx =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
  }

  private Matrix getFrameToCanvasMatrix() {
    return frameToCanvasMatrix;
  }

  public synchronized void drawDebug(final Canvas canvas) {
    final Paint textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(60.0f);

    final Paint boxPaint = new Paint();
    boxPaint.setColor(Color.RED);
    boxPaint.setAlpha(200);
    boxPaint.setStyle(Style.STROKE);

    for (final Pair<Float, RectF> detection : screenRects) {
      final RectF rect = detection.second;
      canvas.drawRect(rect, boxPaint);
      canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
      borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
    }

    if (objectTracker == null) {
      return;
    }

    for (final TrackedRecognition recognition : trackedObjects) {
      final ObjectTracker.TrackedObject trackedObject = recognition.trackedObject;

      final RectF trackedPos = trackedObject.getTrackedPositionInPreviewFrame();

      if (getFrameToCanvasMatrix().mapRect(trackedPos)) {
        final String labelString = String.format("%.2f", trackedObject.getCurrentCorrelation());
        borderedText.drawText(canvas, trackedPos.right, trackedPos.bottom, labelString);
      }
    }

    final Matrix matrix = getFrameToCanvasMatrix();
    objectTracker.drawDebug(canvas, matrix);
  }

  public synchronized void trackResults(
          final List<Recognition> results, final byte[] frame, final long timestamp) {
    processResults(timestamp, results, frame);
  }

  public synchronized RectF getBox()
  {
    if(location != null)
      return location;

    return null;
  }

  public synchronized void draw(final Canvas canvas) {
    final boolean rotated = sensorOrientation % 180 == 90;
    final float multiplier =
            Math.min(canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                    canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
    frameToCanvasMatrix =
            ImageUtils.getTransformationMatrix(
                    frameWidth,
                    frameHeight,
                    (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                    (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                    sensorOrientation,
                    false);
    for (final TrackedRecognition recognition : trackedObjects) {
      final RectF trackedPos =
              (objectTracker != null)
                      ? recognition.trackedObject.getTrackedPositionInPreviewFrame()
                      : new RectF(recognition.location);

      getFrameToCanvasMatrix().mapRect(trackedPos);
      boxPaint.setColor(recognition.color);

      final float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
      canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);

//      final String labelString = String.format("차량 번호판");
//      borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top - 15, labelString);

      location = trackedPos;
    }
  }

  private boolean initialized = false;

  public synchronized void onFrame(
          final int w,
          final int h,
          final int rowStride,
          final int sensorOrientation,
          final byte[] frame,
          final long timestamp) {
    if (objectTracker == null && !initialized) {
      ObjectTracker.clearInstance();

      objectTracker = ObjectTracker.getInstance(w, h, rowStride, true);
      frameWidth = w;
      frameHeight = h;
      this.sensorOrientation = sensorOrientation;
      initialized = true;

      if (objectTracker == null) {
        String message ="분석 지원 가능한 모듈이 없습니다.";
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
      }
    }

    if (objectTracker == null) {
      return;
    }

    objectTracker.nextFrame(frame, null, timestamp, null, true);

    final LinkedList<TrackedRecognition> copyList =
            new LinkedList<TrackedRecognition>(trackedObjects);
    for (final TrackedRecognition recognition : copyList) {
      final ObjectTracker.TrackedObject trackedObject = recognition.trackedObject;
      final float correlation = trackedObject.getCurrentCorrelation();
      if (correlation < MIN_CORRELATION) {
        trackedObject.stopTracking();
        trackedObjects.remove(recognition);
        availableColors.add(recognition.color);
      }
    }
  }

  private void processResults(
          final long timestamp, final List<Recognition> results, final byte[] originalFrame) {
    final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<Pair<Float, Recognition>>();

    screenRects.clear();
    final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

    for (final Recognition result : results) {
      if (result.getLocation() == null) {
        continue;
      }

      final RectF detectionFrameRect = new RectF(result.getLocation());
      final RectF detectionScreenRect = new RectF();

      rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

      screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

      if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
        continue;
      }

      rectsToTrack.add(new Pair<Float, Recognition>(result.getConfidence(), result));
    }

    if (rectsToTrack.isEmpty()) {
      return;
    }

    if (objectTracker == null) {
      trackedObjects.clear();
      for (final Pair<Float, Recognition> potential : rectsToTrack) {
        final TrackedRecognition trackedRecognition = new TrackedRecognition();
        trackedRecognition.detectionConfidence = potential.first;
        trackedRecognition.location = new RectF(potential.second.getLocation());
        trackedRecognition.trackedObject = null;
        trackedRecognition.title = potential.second.getTitle();
        trackedRecognition.color = COLORS[trackedObjects.size()];
        trackedObjects.add(trackedRecognition);

        if (trackedObjects.size() >= COLORS.length) {
          break;
        }
      }
      return;
    }

    for (final Pair<Float, Recognition> potential : rectsToTrack) {
      handleDetection(originalFrame, timestamp, potential);
    }
  }

  private void handleDetection(
          final byte[] frameCopy, final long timestamp, final Pair<Float, Recognition> potential) {
    final ObjectTracker.TrackedObject potentialObject =
            objectTracker.trackObject(potential.second.getLocation(), timestamp, frameCopy);

    final float potentialCorrelation = potentialObject.getCurrentCorrelation();


    if (potentialCorrelation < MARGINAL_CORRELATION) {
      potentialObject.stopTracking();
      return;
    }

    final List<TrackedRecognition> removeList = new LinkedList<TrackedRecognition>();

    float maxIntersect = 0.0f;

    TrackedRecognition recogToReplace = null;

    for (final TrackedRecognition trackedRecognition : trackedObjects) {
      final RectF a = trackedRecognition.trackedObject.getTrackedPositionInPreviewFrame();
      final RectF b = potentialObject.getTrackedPositionInPreviewFrame();
      final RectF intersection = new RectF();
      final boolean intersects = intersection.setIntersect(a, b);

      final float intersectArea = intersection.width() * intersection.height();
      final float totalArea = a.width() * a.height() + b.width() * b.height() - intersectArea;
      final float intersectOverUnion = intersectArea / totalArea;

      if (intersects && intersectOverUnion > MAX_OVERLAP) {
        if (potential.first < trackedRecognition.detectionConfidence
                && trackedRecognition.trackedObject.getCurrentCorrelation() > MARGINAL_CORRELATION) {
          potentialObject.stopTracking();
          return;
        } else {
          removeList.add(trackedRecognition);

          if (intersectOverUnion > maxIntersect) {
            maxIntersect = intersectOverUnion;
            recogToReplace = trackedRecognition;
          }
        }
      }
    }

    if (availableColors.isEmpty() && removeList.isEmpty()) {
      for (final TrackedRecognition candidate : trackedObjects) {
        if (candidate.detectionConfidence < potential.first) {
          if (recogToReplace == null
                  || candidate.detectionConfidence < recogToReplace.detectionConfidence) {
            recogToReplace = candidate;
          }
        }
      }
      if (recogToReplace != null) {
        removeList.add(recogToReplace);
      } else {
      }
    }

    for (final TrackedRecognition trackedRecognition : removeList) {

      trackedRecognition.trackedObject.stopTracking();
      trackedObjects.remove(trackedRecognition);
      if (trackedRecognition != recogToReplace) {
        availableColors.add(trackedRecognition.color);
      }
    }

    if (recogToReplace == null && availableColors.isEmpty()) {
      potentialObject.stopTracking();
      return;
    }

    final TrackedRecognition trackedRecognition = new TrackedRecognition();
    trackedRecognition.detectionConfidence = potential.first;
    trackedRecognition.trackedObject = potentialObject;
    trackedRecognition.title = potential.second.getTitle();

    trackedRecognition.color =
            recogToReplace != null ? recogToReplace.color : availableColors.poll();
    trackedObjects.add(trackedRecognition);
  }
}