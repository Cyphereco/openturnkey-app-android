package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.util.Objects;


public class ActivityChooseKey extends AppCompatActivity {
    public static final String TAG = ActivityChooseKey.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);

    private static final int ZXING_CAMERA_PERMISSION = 1;
    public static final int REQUEST_CODE_QR_CODE = 0;
    public static final String KEY_PATH = "KEY_PATH";

    private boolean isValidL1 = false;
    private boolean isValidL2 = false;
    private boolean isValidL3 = false;
    private boolean isValidL4 = false;
    private boolean isValidL5 = false;

    private boolean isAllPathValid() {
        return (isValidL1 && isValidL2 && isValidL3 && isValidL4 && isValidL5);
    }

    private void processKeyPathChanged() {
        Button btn = findViewById(R.id.buttonChooseKeyOk);
        if (isAllPathValid()) {
            btn.setEnabled(true);
            btn.setAlpha(1.0f);
        } else {
            btn.setEnabled(false);
            btn.setAlpha(.5f);
        }
    }

    public void launchQRcodeScanActivity(View v) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent(getApplicationContext(), ActivityQRcodeScan.class);
            startActivityForResult(intent, ActivityChooseKey.REQUEST_CODE_QR_CODE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logger.debug("onActivityResult:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                // Handle successful scan
                String contents = intent.getStringExtra(MainActivity.KEY_QR_CODE);
                updateKeyPathData(contents);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.info("onCreate");
        setContentView(R.layout.activity_choose_key);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        ImageView iv;
        iv = findViewById(R.id.imageChooseKeyScanQRCode);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchQRcodeScanActivity(view);
            }
        });

        iv = findViewById(R.id.imageChooseKeyPasteClipboard);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard.hasPrimaryClip()) {
                    android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
                    android.content.ClipData data = clipboard.getPrimaryClip();
                    if (data != null && description != null) {
                        String contents = data.getItemAt(0).getText().toString();
                        updateKeyPathData(contents);
                    }
                }
            }
        });

        EditText etL1 = findViewById(R.id.editTextChooseKeyL1);
        etL1.addTextChangedListener(new TextWatcherKeyPath(etL1) {
            @Override
            public void afterTextChanged(Editable editable) {
                super.afterTextChanged(editable);
                isValidL1 = isKeyPathValid();
                processKeyPathChanged();
            }
        });

        EditText etL2 = findViewById(R.id.editTextChooseKeyL2);
        etL2.addTextChangedListener(new TextWatcherKeyPath(etL2) {
            @Override
            public void afterTextChanged(Editable editable) {
                super.afterTextChanged(editable);
                isValidL2 = isKeyPathValid();
                processKeyPathChanged();
            }
        });

        EditText etL3 = findViewById(R.id.editTextChooseKeyL3);
        etL3.addTextChangedListener(new TextWatcherKeyPath(etL3) {
            @Override
            public void afterTextChanged(Editable editable) {
                super.afterTextChanged(editable);
                isValidL3 = isKeyPathValid();
                processKeyPathChanged();
            }
        });

        EditText etL4 = findViewById(R.id.editTextChooseKeyL4);
        etL4.addTextChangedListener(new TextWatcherKeyPath(etL4) {
            @Override
            public void afterTextChanged(Editable editable) {
                super.afterTextChanged(editable);
                isValidL4 = isKeyPathValid();
                processKeyPathChanged();
            }
        });

        EditText etL5 = findViewById(R.id.editTextChooseKeyL5);
        etL5.addTextChangedListener(new TextWatcherKeyPath(etL5) {
            @Override
            public void afterTextChanged(Editable editable) {
                super.afterTextChanged(editable);
                isValidL5 = isKeyPathValid();
                processKeyPathChanged();
            }
        });

        Button btnOk = findViewById(R.id.buttonChooseKeyOk);
        btnOk.setEnabled(false);
        btnOk.setAlpha(.5f);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent();
                //Construct path
                EditText etL1 = findViewById(R.id.editTextChooseKeyL1);
                String l1 = etL1.getText().toString();
                EditText etL2 = findViewById(R.id.editTextChooseKeyL2);
                String l2 = etL2.getText().toString();
                EditText etL3 = findViewById(R.id.editTextChooseKeyL3);
                String l3 = etL3.getText().toString();
                EditText etL4 = findViewById(R.id.editTextChooseKeyL4);
                String l4 = etL4.getText().toString();
                EditText etL5 = findViewById(R.id.editTextChooseKeyL5);
                String l5 = etL5.getText().toString();
                String path = l1 + "," + l2 + "," + l3 + "," + l4 + "," + l5;
                logger.debug("Choose key:{}", path);
                intent.putExtra(KEY_PATH, path);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        Button btnCancel = findViewById(R.id.buttonChooseKeyCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFmtErrorDialog(String contents) {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.invalid_key_path)
                .setMessage(contents)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;

        dialog.show();
    }

    private void updateKeyPathData(String contents) {
        logger.info("path:{}", contents);
        // parse path
        int prefixStart = contents.indexOf("m/");
        if (prefixStart != 0) {
            showFmtErrorDialog(contents);
            return;
        }
        int slash = contents.indexOf("/", prefixStart + 2);
        String l1 = contents.substring(prefixStart + 2, slash);

        int nextSlash = contents.indexOf("/", slash + 1);
        String l2 = contents.substring(slash + 1, nextSlash);

        slash = nextSlash;
        nextSlash = contents.indexOf("/", slash + 1);
        String l3 = contents.substring(slash + 1, nextSlash);

        slash = nextSlash;
        nextSlash = contents.indexOf("/", slash + 1);
        String l4 = contents.substring(slash + 1, nextSlash);

        slash = nextSlash;
        String l5 = contents.substring(slash + 1);

        // Check
        try {
            Long.parseLong(l1);
            Long.parseLong(l2);
            Long.parseLong(l3);
            Long.parseLong(l4);
            Long.parseLong(l5);
        } catch (Exception e) {
            showFmtErrorDialog(contents);
            return;
        }
        //Set to edit text
        EditText etL1 = findViewById(R.id.editTextChooseKeyL1);
        etL1.setText(l1);
        EditText etL2 = findViewById(R.id.editTextChooseKeyL2);
        etL2.setText(l2);
        EditText etL3 = findViewById(R.id.editTextChooseKeyL3);
        etL3.setText(l3);
        EditText etL4 = findViewById(R.id.editTextChooseKeyL4);
        etL4.setText(l4);
        EditText etL5 = findViewById(R.id.editTextChooseKeyL5);
        etL5.setText(l5);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.setCurrentActivity(getClass().getName());
    }
}
