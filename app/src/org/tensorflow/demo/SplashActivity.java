package org.tensorflow.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstance) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstance);
        try {
            Thread.sleep(2000);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}