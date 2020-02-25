package com.cyphereco.openturnkey.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;


public class SplashActivity extends AppCompatActivity {
    public static final String TAG = SplashActivity.class.getSimpleName();
    Logger logger;

    private static int SPLASH_TIMEOUT = 1000;
    static int i = 0;
    private boolean isStarted = false;
    private static final int REQUEST_CODE = 0x11;

    String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            logger = Log4jHelper.getLogger(TAG);

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                logger.info("Permission granted");
            }
            else {
                logger.info("Permission is not granted");
            }

            if (MainActivity.isRunning()) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                return;
            }
            SplashLauncher splashLauncher = new SplashLauncher();
            splashLauncher.start();
        }
    }

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Configurations.writeLogToFile) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE); // without sdk version check

        }
        else {
            logger = Log4jHelper.getLogger(TAG);
            if (MainActivity.isRunning()) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                return;
            }
            SplashLauncher splashLauncher = new SplashLauncher();
            splashLauncher.start();
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
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
        if (MainActivity.isRunning()) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    /* Idle thread for loading MainActivity after SPLASH_TIMEOUT */
    private class SplashLauncher extends Thread {
        @Override
        public void run() {
            logger.info("Start MainActivity in {} seconds", SPLASH_TIMEOUT / 1000);

            try {
                sleep(SPLASH_TIMEOUT);
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            isStarted = true;
            logger.info("MainActivity is starting...");
        }
    }
}
