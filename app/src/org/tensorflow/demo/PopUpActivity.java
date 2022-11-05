package org.tensorflow.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class PopUpActivity extends Activity implements View.OnClickListener{
    private DatabaseReference mPostReference;

    Button btn_Update;

    EditText edit_ID;
    EditText edit_PW;
    EditText edit_Name;
    EditText edit_Phone;
    EditText edit_Email;
    EditText edit_Address;
    EditText edit_plate;
    EditText edit_car;

    TextView text_ID;
    TextView text_PW;
    TextView text_Name;
    TextView text_Phone;
    TextView text_Email;
    TextView text_Address;
    TextView text_Gender;
    TextView text_plate;
    TextView text_car;

    Button check_Man;
    Button check_Woman;

    public String ID;
    public String PW;
    public String name;
    public String phone;
    public String email;
    public String address;
    public String gender = "";
    public String plate;
    public String car;
    String sort = "id";
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseReference = FirebaseDatabase.getInstance().getReference("id_list");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.popup_activity);

        btn_Update = (Button) findViewById(R.id.btn_update);
        btn_Update.setOnClickListener(this);

        edit_ID = (EditText) findViewById(R.id.edit_id);
        edit_PW = (EditText) findViewById(R.id.edit_pw);
        edit_Name = (EditText) findViewById(R.id.edit_name);
        edit_Phone = (EditText) findViewById(R.id.edit_phone);
        edit_Email = (EditText) findViewById(R.id.edit_email);
        edit_Address = (EditText) findViewById(R.id.edit_address);
        edit_plate = (EditText) findViewById(R.id.edit_plate);
        edit_car = (EditText) findViewById(R.id.edit_car);

        text_ID = (TextView) findViewById(R.id.text_id);
        text_PW = (TextView) findViewById(R.id.text_pw);
        text_Name = (TextView) findViewById(R.id.text_name);
        text_Phone = (TextView) findViewById(R.id.text_phone);
        text_Email = (TextView) findViewById(R.id.text_email);
        text_Address = (TextView) findViewById(R.id.text_address);
        text_Gender= (TextView) findViewById(R.id.text_gender);
        text_plate= (TextView) findViewById(R.id.text_plate);
        text_car= (TextView) findViewById(R.id.text_car);

        check_Man = (Button) findViewById(R.id.check_man);
        check_Man.setOnClickListener(this);
        check_Woman = (Button) findViewById(R.id.check_woman);
        check_Woman.setOnClickListener(this);
        Intent intent = getIntent();
        final String name = intent.getExtras().getString("id");
        databaseReference.orderByChild("id").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                FirebasePost user = dataSnapshot.getValue(FirebasePost.class);
                if (user.id.equals(name)) {
                    edit_ID.setText(user.id);
                    edit_PW.setText(user.pw);
                    edit_Name.setText(user.name);
                    edit_Phone.setText(user.phone);
                    edit_Email.setText(user.email);
                    edit_Address.setText(user.address);
                    if (user.gender.equals("Man")) {
                        check_Man.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.man_on));
                        check_Woman.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.woman_off));
                        gender = "Man";
                    } else {
                        check_Man.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.man_off));
                        check_Woman.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.woman_on));
                        gender = "Woman";
                    }
                    edit_plate.setText(user.plate);
                    edit_car.setText(user.car);
                    edit_ID.setEnabled(false);
                    btn_Update.setEnabled(true);
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
        });    }

    //확인 버튼 클릭
    public void mOnClose(View v){
        //데이터 전달하기
        Intent intent = new Intent(this, DetectorActivity.class);
        intent.putExtra("id", ID);
        //액티비티(팝업) 닫기
        finish();
        startActivity(intent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //바깥레이어 클릭시 안닫히게
        if(event.getAction()==MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //안드로이드 백버튼 막기
        return;
    }

    public void postFirebaseDatabase(boolean add){
        mPostReference = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> postValues = null;
        if(add){
            FirebasePost post = new FirebasePost(ID, PW, name, phone, email, address, gender, plate, car);
            postValues = post.toMap();
        }
        childUpdates.put("/id_list/" + ID, postValues);
        mPostReference.updateChildren(childUpdates);
    }

    public void getFirebaseDatabase(){
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        Query sortbyAge = FirebaseDatabase.getInstance().getReference().child("id_list").orderByChild(sort);
        sortbyAge.addListenerForSingleValueEvent(postListener);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_update:
                ID = edit_ID.getText().toString();
                PW = edit_PW.getText().toString();
                name = edit_Name.getText().toString();
                phone = edit_Phone.getText().toString();
                email = edit_Email.getText().toString();
                address = edit_Address.getText().toString();
                plate = edit_plate.getText().toString();
                car = edit_car.getText().toString();
                postFirebaseDatabase(true);
                getFirebaseDatabase();
                edit_ID.setEnabled(false);
                edit_ID.requestFocus();
                edit_ID.setCursorVisible(true);
                Toast.makeText(this,"수정이 완료되었습니다.",Toast.LENGTH_SHORT).show();
                //데이터 전달하기
                Intent intent = new Intent(this, DetectorActivity.class);
                intent.putExtra("id", ID);
                //액티비티(팝업) 닫기
                finish();
                startActivity(intent);
                break;

            case R.id.check_man:
                check_Man.setBackground(ContextCompat.getDrawable(this,R.drawable.man_on));
                check_Woman.setBackground(ContextCompat.getDrawable(this,R.drawable.woman_off));
                gender = "Man";
                break;

            case R.id.check_woman:
                check_Man.setBackground(ContextCompat.getDrawable(this,R.drawable.man_off));
                check_Woman.setBackground(ContextCompat.getDrawable(this,R.drawable.woman_on));
                gender = "Woman";
                break;
        }
    }
}
