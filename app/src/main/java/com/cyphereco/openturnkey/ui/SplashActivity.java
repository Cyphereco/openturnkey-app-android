package com.cyphereco.openturnkey.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.cyphereco.openturnkey.R;

public class SplashActivity extends AppCompatActivity {

    private static int SPLASH_TIMEOUT = 3000;
    static int i = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (MainActivity.isRunning()) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        SplashLauncher splashLauncher = new SplashLauncher();
        splashLauncher.start();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals("android.intent.action.MAIN")) {
            return;
        }
        if (OpenturnkeyInfoActivity.isActive()) {
            intent.setClass(SplashActivity.this, OpenturnkeyInfoActivity.class);
            startActivity(intent);
        }
        else {
            intent.setClass(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MainActivity.isRunning()) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    private class SplashLauncher extends Thread {
        @Override
        public void run() {
            try {
                sleep(SPLASH_TIMEOUT);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }
}
