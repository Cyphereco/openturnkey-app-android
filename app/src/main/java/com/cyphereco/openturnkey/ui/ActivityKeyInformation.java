package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.protocol.SessionData;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.QRCodeUtils;

import org.slf4j.Logger;

import java.util.Objects;

public class ActivityKeyInformation extends AppCompatActivity {
    public static final String TAG = ActivityKeyInformation.class.getSimpleName();
    static Logger logger = Log4jHelper.getLogger(TAG);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_keys);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (intent == null) {
            logger.error("Intent is NULL");
            return;
        }

        final OtkData otkData = (OtkData) intent.getSerializableExtra(MainActivity.KEY_OTK_DATA);
        if (otkData == null) {
            logger.error("OtkData is NULL");
            return;
        }

        updateInfo(otkData);
        setButtonListener();

        TextView txt = findViewById(R.id.how_to_validate);
        txt.setMovementMethod(LinkMovementMethod.getInstance());
        txt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String url = "https://openturnkey.com/faq#comp-k3vbamb6";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateInfo(final OtkData otkData) {
        SessionData sessionData;
        TextView tv;
        EditText et;

        sessionData = otkData.getSessionData();
        /* Set master public key */
        tv = findViewById(R.id.ki_tv_master_pk_content);
        tv.setText(sessionData.getMasterExtKey());
        /* Set derivative public key */
        tv = findViewById(R.id.ki_tv_derivative_pk_content);
        tv.setText(sessionData.getDerivativeExtKey());
        /* Get key path */
        String[] keyPath = sessionData.getDerivativePath().split("/");
        et = findViewById(R.id.ki_tx_derivative_key_path_l1);
        et.setText(keyPath[1]);
        et = findViewById(R.id.ki_tx_derivative_key_path_l2);
        et.setText(keyPath[2]);
        et = findViewById(R.id.ki_tx_derivative_key_path_l3);
        et.setText(keyPath[3]);
        et = findViewById(R.id.ki_tx_derivative_key_path_l4);
        et.setText(keyPath[4]);
        et = findViewById(R.id.ki_tx_derivative_key_path_l5);
        et.setText(keyPath[5]);
    }

    private void setButtonListener() {
        Button btn;
        ImageView iv;

        btn = findViewById(R.id.ki_btn_ok);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        /* Copy master key */
        iv = findViewById(R.id.ki_iv_icon_master_pk_copy);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = findViewById(R.id.ki_tv_master_pk_content);
                copyText(getString(R.string.master_public_key), tv.getText().toString());
            }
        });

        /* Copy derivative key */
        iv = findViewById(R.id.ki_iv_icon_derivative_pk_copy);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = findViewById(R.id.ki_tv_derivative_pk_content);
                copyText(getString(R.string.derivative_public_key), tv.getText().toString());
            }
        });

        /* Copy key path */
        iv = findViewById(R.id.ki_iv_icon_derivative_pk_path_copy);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyText(getString(R.string.derivative_key_paths), getKeyPathString());
            }
        });

        /* Show master key QR Code */
        iv = findViewById(R.id.ki_iv_icon_master_pk_qrcode);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = findViewById(R.id.ki_tv_master_pk_content);
                showQRCode(getString(R.string.master_public_key), tv.getText().toString());
            }
        });

        /* Show derivative key QR Code */
        iv = findViewById(R.id.ki_iv_icon_derivative_pk_qrcode);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView tv = findViewById(R.id.ki_tv_derivative_pk_content);
                showQRCode(getString(R.string.derivative_public_key), tv.getText().toString());
            }
        });

        /* Show key path QR Code */
        iv = findViewById(R.id.ki_iv_icon_derivative_pk_path_qrcode);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQRCode(getString(R.string.derivative_key_paths), getKeyPathString());
            }
        });
    }

    private String getKeyPathString() {
        String path = "m/";
        EditText et;

        et = findViewById(R.id.ki_tx_derivative_key_path_l1);
        path += et.getText().toString() + "/";
        et = findViewById(R.id.ki_tx_derivative_key_path_l2);
        path += et.getText().toString() + "/";
        et = findViewById(R.id.ki_tx_derivative_key_path_l3);
        path += et.getText().toString() + "/";
        et = findViewById(R.id.ki_tx_derivative_key_path_l4);
        path += et.getText().toString() + "/";
        et = findViewById(R.id.ki_tx_derivative_key_path_l5);
        path += et.getText().toString();

        return path;
    }

    private void copyText(String label, String text) {
        if ((null == label) || (null == text)) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        AlertPrompt.info(getApplicationContext(), label + getString(R.string.is_copied));
    }

    private void showQRCode(String dialogTitle, String text) {
        final View v = View.inflate(this, R.layout.dialog_key_qrcode, null);
        TextView tv = v.findViewById(R.id.dialog_key_qrcode_tv_content);
        ImageView ivQRCode = v.findViewById(R.id.dialog_key_qrcode_iv_qrcode);

        Bitmap bitmap = QRCodeUtils.encodeAsBitmap(text,
                ivQRCode.getDrawable().getIntrinsicWidth(),
                ivQRCode.getDrawable().getIntrinsicHeight());
        ivQRCode.setImageBitmap(bitmap);
        tv.setText(text);
        new AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setView(v)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.setCurrentActivity(getClass().getName());
    }
}
