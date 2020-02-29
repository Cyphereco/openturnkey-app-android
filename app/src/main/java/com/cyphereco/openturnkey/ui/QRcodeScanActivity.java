package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.ArrayList;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import com.cyphereco.openturnkey.R;

public class QRcodeScanActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private static final int ZXING_CAMERA_PERMISSION = 1;

    private ZXingScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_qrcode_scan);

        ViewGroup contentFrame = findViewById(R.id.content_frame);
        mScannerView = new ZXingScannerView(this);
        contentFrame.addView(mScannerView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            this.finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        }

        mScannerView.setResultHandler(this);
        mScannerView.setSquareViewFinder(true);
        ArrayList l = new ArrayList();
        l.add(BarcodeFormat.QR_CODE);
        mScannerView.setFormats(l);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        //Toast.makeText(this, "Contents = " + rawResult.getText() +
        //        ", Format = " + rawResult.getBarcodeFormat().toString(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent();
        intent.putExtra(MainActivity.KEY_QR_CODE, rawResult.getText());
        setResult(RESULT_OK, intent);
        finish();
    }
}
