package org.tensorflow.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends Activity {
    public static Activity loginActivity;

    private DatabaseReference databaseReference;

    private EditText editTextId;
    private EditText editTextPassword;

    Button login;
    Button join;

    boolean islogin = false;

    private String ID = null;
    private String password = null;
    private Bundle savedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginActivity = LoginActivity.this;
        databaseReference = FirebaseDatabase.getInstance().getReference("id_list");

        editTextId = (EditText) findViewById(R.id.et_id);
        editTextPassword = (EditText) findViewById(R.id.et_password);

        ID = editTextId.getText().toString();
        password = editTextPassword.getText().toString();


        login = (Button) findViewById(R.id.btn_signIn);
        login.setEnabled(false);

        editTextId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ID = editTextId.getText().toString();
                password = editTextPassword.getText().toString();
                if(ID != null && password != null){
                    login.setEnabled(true);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FirebasePost user = dataSnapshot.child(editTextId.getText().toString()).getValue(FirebasePost.class);
                        if (dataSnapshot.child(editTextId.getText().toString()).exists()) {
                            if (user.getPw().equals(editTextPassword.getText().toString())) {
                                Toast.makeText(LoginActivity.this, R.string.success_login, Toast.LENGTH_SHORT).show();

                                islogin = true;
                                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                                intent.putExtra("id", ID);
                                startActivity(intent);
                            } else if (!(user.getPw().equals(editTextPassword.getText().toString()))) {
                                Toast.makeText(LoginActivity.this, "ID 또는 비밀번호 오류입니다.", Toast.LENGTH_SHORT).show();

                                islogin = false;
                            }
                        }
                        else if(!(dataSnapshot.child(editTextId.getText().toString()).exists()) || !(user.getPw().equals(editTextPassword.getText().toString()))){
                            Toast.makeText(LoginActivity.this, "ID 또는 비밀번호 오류입니다.", Toast.LENGTH_SHORT).show();

                            islogin = false;
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });

        join = (Button) findViewById(R.id.btn_signUp);
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), JoinActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        if(islogin) {
            Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
            startActivity(intent);
        }
    }

}