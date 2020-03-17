package com.cyphereco.openturnkey.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;


public class ActivitySplash extends AppCompatActivity {
    public static final String TAG = ActivitySplash.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);

    private static final int REQUEST_CODE = 0x11;

    String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                logger.debug("Permission Write File Granted");
            } else {
                logger.debug("Permission Write File Revoked");
            }
        }
        SplashLauncher splashLauncher = new SplashLauncher();
        splashLauncher.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Customize Screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Configurations.writeLogToFile) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE); // without sdk version check
        }
        else {
            SplashLauncher splashLauncher = new SplashLauncher();
            splashLauncher.start();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        gotoAcitivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gotoAcitivity();
    }

    private void gotoAcitivity() {
        if (MainActivity.getCurrentActivity() != null) {
            // app is already running, switch to current activity
            Class cls = null;

            try {
                cls = Class.forName(MainActivity.getCurrentActivity());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (cls != null) {
                startActivity(new Intent(this, cls));
            }
        }
    }

    /* Idle thread for loading MainActivity after SPLASH_TIMEOUT */
    private class SplashLauncher extends Thread {
        @Override
        public void run() {
            int SPLASH_TIMEOUT = 1000;
            logger.debug("Show Splash");

            try {
                sleep(SPLASH_TIMEOUT);
            } catch (Exception e) {
                logger.error("Exception:", e);
            }

            Intent intent = new Intent(ActivitySplash.this, MainActivity.class);
            startActivity(intent);

            logger.debug("Start MainActivity");

            finish();
        }
    }
}
