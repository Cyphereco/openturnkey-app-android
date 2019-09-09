package com.cyphereco.openturnkey.ui;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.cyphereco.openturnkey.R;

public class SplashActivity extends AppCompatActivity {
    public static final String TAG = SplashActivity.class.getSimpleName();
    private static int SPLASH_TIMEOUT = 3000;
    static int i = 0;
    private boolean isStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
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
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent():" + intent.getAction());
        if (isStarted == false) {
            // Wait  few seconds and call onNewIntent again.
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onNewIntent(intent);
                }
            }, 1000);
            return;
        }

        // No need to process MAIN action
        if (intent.getAction().equals("android.intent.action.MAIN")) {
            return;
        }
        // Should be NFC related intents
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
        Log.d(TAG, "onResume");
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
            isStarted = true;
        }
    }
}
