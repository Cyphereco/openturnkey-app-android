package com.cyphereco.openturnkey.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.cyphereco.openturnkey.R;

public class SplashActivity extends AppCompatActivity {

    private static int SPLASH_TIMEOUT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        SplashLauncher splashLauncher = new SplashLauncher();
        splashLauncher.start();

    }

    private class SplashLauncher extends Thread{
        @Override
        public void run() {
            try {
                sleep(SPLASH_TIMEOUT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            SplashActivity.this.finish();
        }
    }
}
