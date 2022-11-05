//package org.tensorflow.demo;
//
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.CheckBox;
//import android.widget.EditText;
//import android.widget.ListView;
//import android.widget.TextView;
//import android.widget.Toast;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.Query;
//import com.google.firebase.database.ValueEventListener;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//public class JoinActivity extends Activity implements View.OnClickListener{
//
//    private DatabaseReference mPostReference;
//
//    Button btn_duplicate;
//    Button btn_Update;
//    Button btn_Insert;
//    Button btn_Select;
//
//    EditText edit_ID;
//    EditText edit_PW;
//    EditText edit_Name;
//    EditText edit_Phone;
//    EditText edit_Email;
//    EditText edit_Address;
//    EditText edit_plate;
//    EditText edit_car;
//
//    TextView text_ID;
//    TextView text_PW;
//    TextView text_Name;
//    TextView text_Phone;
//    TextView text_Email;
//    TextView text_Address;
//    TextView text_Gender;
//    TextView text_plate;
//    TextView text_car;
//
//    CheckBox check_Man;
//    CheckBox check_Woman;
//    CheckBox check_ID;
//    CheckBox check_Name;
//
//    public String ID;
//    public String PW;
//    public String name;
//    public String phone;
//    public String email;
//    public String address;
//    public String gender = "";
//    public String plate;
//    public String car;
//    String sort = "id";
//
//    ArrayAdapter<String> arrayAdapter;
//
//    static ArrayList<String> arrayIndex =  new ArrayList<String>();
//    static ArrayList<String> arrayData = new ArrayList<String>();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_join);
//
//        btn_duplicate = (Button) findViewById(R.id.btn_duplicate);
//        btn_duplicate.setOnClickListener(this);
//        btn_Insert = (Button) findViewById(R.id.btn_insert);
//        btn_Insert.setOnClickListener(this);
//        btn_Update = (Button) findViewById(R.id.btn_update);
//        btn_Update.setOnClickListener(this);
//        btn_Select = (Button) findViewById(R.id.btn_select);
//        btn_Select.setOnClickListener(this);
//
//        edit_ID = (EditText) findViewById(R.id.edit_id);
//        edit_PW = (EditText) findViewById(R.id.edit_pw);
//        edit_Name = (EditText) findViewById(R.id.edit_name);
//        edit_Phone = (EditText) findViewById(R.id.edit_phone);
//        edit_Email = (EditText) findViewById(R.id.edit_email);
//        edit_Address = (EditText) findViewById(R.id.edit_address);
//        edit_plate = (EditText) findViewById(R.id.edit_plate);
//        edit_car = (EditText) findViewById(R.id.edit_car);
//
//        text_ID = (TextView) findViewById(R.id.text_id);
//        text_PW = (TextView) findViewById(R.id.text_pw);
//        text_Name = (TextView) findViewById(R.id.text_name);
//        text_Phone = (TextView) findViewById(R.id.text_phone);
//        text_Email = (TextView) findViewById(R.id.text_email);
//        text_Address = (TextView) findViewById(R.id.text_address);
//        text_Gender= (TextView) findViewById(R.id.text_gender);
//        text_plate= (TextView) findViewById(R.id.text_plate);
//        text_car= (TextView) findViewById(R.id.text_car);
//
//        check_Man = (CheckBox) findViewById(R.id.check_man);
//        check_Man.setOnClickListener(this);
//        check_Woman = (CheckBox) findViewById(R.id.check_woman);
//        check_Woman.setOnClickListener(this);
//        check_ID = (CheckBox) findViewById(R.id.check_userid);
//        check_ID.setOnClickListener(this);
//        check_Name = (CheckBox) findViewById(R.id.check_name);
//        check_Name.setOnClickListener(this);
//
//        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
//        ListView listView = (ListView) findViewById(R.id.db_list_view);
//        listView.setAdapter(arrayAdapter);
//        listView.setOnItemClickListener(onClickListener);
//        listView.setOnItemLongClickListener(ClickListener);
//
//        check_ID.setChecked(true);
//        getFirebaseDatabase();
//        listView.setEnabled(false);
//        btn_duplicate.setEnabled(true);
//        btn_Insert.setEnabled(true);
//        btn_Update.setEnabled(false);
//    }
//
//    public void setInsertMode(){
//        edit_ID.setText("");
//        edit_PW.setText("");
//        edit_Name.setText("");
//        edit_Phone.setText("");
//        edit_Email.setText("");
//        edit_Address.setText("");
//        check_Man.setChecked(true);
//        check_Woman.setChecked(false);
//        edit_plate.setText("");
//        edit_car.setText("");
//        btn_Insert.setEnabled(true);
//        btn_Update.setEnabled(false);
//    }
//
//
//    private AdapterView.OnItemClickListener onClickListener = new AdapterView.OnItemClickListener() {
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            String[] tempData = arrayData.get(position).split("\\s+");
//            edit_ID.setText(tempData[0].trim());
//            edit_PW.setText(tempData[1].trim());
//            edit_Name.setText(tempData[2].trim());
//            edit_Phone.setText(tempData[3].trim());
//            edit_Email.setText(tempData[4].trim());
//            edit_Address.setText(tempData[5].trim());
//            if(tempData[6].trim().equals("Man")){
//                check_Man.setChecked(true);
//                gender = "Man";
//            }else{
//                check_Woman.setChecked(true);
//                gender = "Woman";
//            }
//            edit_plate.setText(tempData[7].trim());
//            edit_car.setText(tempData[8].trim());
//            edit_ID.setEnabled(false);
//            btn_Insert.setEnabled(false);
//            btn_Update.setEnabled(true);
//        }
//    };
//
//    private AdapterView.OnItemLongClickListener ClickListener = new AdapterView.OnItemLongClickListener() {
//        @Override
//        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//            final String[] nowData = arrayData.get(position).split("\\s+");
//            ID = nowData[0];
//            String viewData = nowData[0] + ", " + nowData[1] + ", " + nowData[2] + ", " + nowData[3] + ", " + nowData[4] + ", " + nowData[5] + ", " + nowData[6] + ", " + nowData[7] + ", " + nowData[8];
//            AlertDialog.Builder dialog = new AlertDialog.Builder(JoinActivity.this);
//            dialog.setTitle("데이터 삭제")
//                    .setMessage("해당 데이터를 삭제 하시겠습니까?" + "\n" + viewData)
//                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            postFirebaseDatabase(false);
//                            getFirebaseDatabase();
//                            setInsertMode();
//                            edit_ID.setEnabled(true);
//                            Toast.makeText(JoinActivity.this, "데이터를 삭제했습니다.", Toast.LENGTH_SHORT).show();
//                        }
//                    })
//                    .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            Toast.makeText(JoinActivity.this, "삭제를 취소했습니다.", Toast.LENGTH_SHORT).show();
//                            setInsertMode();
//                            edit_ID.setEnabled(true);
//                        }
//                    })
//                    .create()
//                    .show();
//            return false;
//        }
//    };
//
//    public boolean IsExistID(){
//        boolean IsExist = arrayIndex.contains(ID);
//        return IsExist;
//    }
//
//    public void postFirebaseDatabase(boolean add){
//        mPostReference = FirebaseDatabase.getInstance().getReference();
//        Map<String, Object> childUpdates = new HashMap<>();
//        Map<String, Object> postValues = null;
//        if(add){
//            FirebasePost post = new FirebasePost(ID, PW, name, phone, email, address, gender, plate, car);
//            postValues = post.toMap();
//        }
//        childUpdates.put("/id_list/" + ID, postValues);
//        mPostReference.updateChildren(childUpdates);
//    }
//
//    public void getFirebaseDatabase(){
//        ValueEventListener postListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                arrayData.clear();
//                arrayIndex.clear();
//                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
//                    String key = postSnapshot.getKey();
//                    FirebasePost get = postSnapshot.getValue(FirebasePost.class);
//                    String[] info = {get.id, get.pw, get.name, get.phone, get.email, get.address, get.gender, get.plate, get.car};
//                    String Result = setTextLength(info[0],20) + setTextLength(info[1],20) + setTextLength(info[2],10) + setTextLength(info[3],15) + setTextLength(info[4],30)+ setTextLength(info[5],100)+ setTextLength(info[6],10)+ setTextLength(info[7],10)+ setTextLength(info[8],30);
//                    arrayData.add(Result);
//                    arrayIndex.add(key);
//                }
//                arrayAdapter.clear();
//                arrayAdapter.addAll(arrayData);
//                arrayAdapter.notifyDataSetChanged();
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//            }
//        };
//        Query sortbyAge = FirebaseDatabase.getInstance().getReference().child("id_list").orderByChild(sort);
//        sortbyAge.addListenerForSingleValueEvent(postListener);
//    }
//
//    public String setTextLength(String text, int length){
//        if(text.length()<length){
//            int gap = length - text.length();
//            for (int i=0; i<gap; i++){
//                text = text + " ";
//            }
//        }
//        return text;
//    }
//
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.btn_duplicate:
//                ID = edit_ID.getText().toString();
//                if(IsExistID()) {
//                    Toast.makeText(JoinActivity.this, "이미 존재하는 ID입니다. 다른 ID로 설정해주세요.", Toast.LENGTH_LONG).show();
//                }
//                else
//                    Toast.makeText(JoinActivity.this, "사용 가능한 ID입니다.", Toast.LENGTH_LONG).show();
//
//                edit_ID.requestFocus();
//                edit_ID.setCursorVisible(true);
//                break;
//
//            case R.id.btn_insert:
//                ID = edit_ID.getText().toString();
//                PW = edit_PW.getText().toString();
//                name = edit_Name.getText().toString();
//                phone = edit_Phone.getText().toString();
//                email = edit_Email.getText().toString();
//                address = edit_Address.getText().toString();
//                plate = edit_plate.getText().toString();
//                car = edit_car.getText().toString();
//                if(!IsExistID()){
//                    postFirebaseDatabase(true);
//                    getFirebaseDatabase();
//                    setInsertMode();
//                }else{
//                    Toast.makeText(JoinActivity.this, "이미 존재하는 ID입니다. 다른 ID로 설정해주세요.", Toast.LENGTH_LONG).show();
//                }
//                edit_ID.requestFocus();
//                edit_ID.setCursorVisible(true);
//                break;
//
//            case R.id.btn_update:
//                ID = edit_ID.getText().toString();
//                PW = edit_PW.getText().toString();
//                name = edit_Name.getText().toString();
//                phone = edit_Phone.getText().toString();
//                email = edit_Email.getText().toString();
//                address = edit_Address.getText().toString();
//                plate = edit_plate.getText().toString();
//                car = edit_car.getText().toString();
//                postFirebaseDatabase(true);
//                getFirebaseDatabase();
//                setInsertMode();
//                edit_ID.setEnabled(true);
//                edit_ID.requestFocus();
//                edit_ID.setCursorVisible(true);
//                break;
//
//            case R.id.btn_select:
//                getFirebaseDatabase();
//                break;
//
//            case R.id.check_man:
//                check_Woman.setChecked(false);
//                gender = "Man";
//                break;
//
//            case R.id.check_woman:
//                check_Man.setChecked(false);
//                gender = "Woman";
//                break;
//
//            case R.id.check_userid:
//                check_Name.setChecked(false);
//                sort = "id";
//                break;
//
//            case R.id.check_name:
//                check_ID.setChecked(false);
//                sort = "name";
//                break;
//        }
//    }
//}
package org.tensorflow.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JoinActivity extends Activity implements View.OnClickListener{

    private DatabaseReference mPostReference;

    Button btn_Insert;

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
    public String gender = "Man";
    public String plate;
    public String car;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        btn_Insert = (Button) findViewById(R.id.btn_insert);
        btn_Insert.setOnClickListener(this);

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

        btn_Insert.setEnabled(true);

    }

    public void setInsertMode(){
        edit_ID.setText("");
        edit_PW.setText("");
        edit_Name.setText("");
        edit_Phone.setText("");
        edit_Email.setText("");
        edit_Address.setText("");
        edit_plate.setText("");
        edit_car.setText("");
        btn_Insert.setEnabled(true);
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

    public String setTextLength(String text, int length){
        if(text.length()<length){
            int gap = length - text.length();
            for (int i=0; i<gap; i++){
                text = text + " ";
            }
        }
        return text;
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_insert:
                ID = edit_ID.getText().toString();
                PW = edit_PW.getText().toString();
                name = edit_Name.getText().toString();
                phone = edit_Phone.getText().toString();
                email = edit_Email.getText().toString();
                address = edit_Address.getText().toString();
                plate = edit_plate.getText().toString();
                car = edit_car.getText().toString();

                postFirebaseDatabase(true);
                setInsertMode();

                edit_ID.requestFocus();
                edit_ID.setCursorVisible(true);

                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
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