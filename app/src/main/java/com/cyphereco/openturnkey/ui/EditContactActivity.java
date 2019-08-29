package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;

import java.util.List;

public class EditContactActivity extends AppCompatActivity {
    private final static String TAG = EditContactActivity.class.getSimpleName();
    private final static int ZXING_CAMERA_PERMISSION = 1;
    private static final String BEGIN_BITCOIN_SIGNED_MESSAGE = "-----BEGIN BITCOIN SIGNED MESSAGE-----";
    private static final String AMOUNT_EQUAL_TO = "amount=";

    public static final String KEY_QR_CODE = "KEY_QR_CODE";

    private TextInputEditText mInputContactName;
    private TextInputEditText mInputContactAddress;
    private OpenturnkeyDB mOtkDB;
    private String mContactName;
    private String mContactAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contact);

        mInputContactName = findViewById(R.id.input_edit_contact_name);
        mInputContactAddress = findViewById(R.id.input_edit_contact_address);

        ImageButton cancelBtn = findViewById(R.id.cancel_btn_edit_contact);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivity();
            }
        });

        ImageButton saveBtn = findViewById(R.id.save_btn_edit_contact);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContact();
            }
        });

        ImageButton delBtn = findViewById(R.id.del_btn_edit_contact);
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteContact();
            }
        });

        ImageView iv = findViewById(R.id.icon_scan_qrcode_edit_contact);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchQRcodeScanActivity(view);
            }
        });

        mOtkDB = new OpenturnkeyDB(getApplicationContext());

        Intent intent = this.getIntent();
        mContactName = intent.getStringExtra("CONTACT_NAME");
        mContactAddress = intent.getStringExtra("CONTACT_ADDRESS");
        mInputContactName.setText(mContactName);
        mInputContactAddress.setText(mContactAddress);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                // Handle successful scan
                String contents = intent.getStringExtra(KEY_QR_CODE);
                if (contents.contains(BEGIN_BITCOIN_SIGNED_MESSAGE)) {
                    // TODO
                    // updateFormattedSignedMessage(contents);
                }
                else {
                    String addr = "";
                    String amount = "0.0";
                    boolean notBTC = false;

                    if (contents.contains(":")) {
                        // contents might be a uri
                        String uriArray[] = contents.split(":");

                        if (uriArray.length > 1) {
                            if (uriArray[0].contentEquals("bitcoin")) {
                                contents = uriArray[1];
                                addr = contents;
                            }
                            else {
                                notBTC = true;
                                Toast.makeText(this, "Sorry! " + uriArray[0] +
                                        " is not supported at this moment.",Toast.LENGTH_LONG).show();
                                contents = "";
                            }
                        }
                        else {
                            // incorrect uri format
                        }
                    }

                    if (!notBTC && contents.contains("?")) {
                        // contents might contains query tag
                        String queryArray[] = contents.split("\\?");

                        if (queryArray.length > 1) {
                            addr = queryArray[0];

                            String queryTagArray[] = queryArray[1].split("&");

                            for (String s : queryTagArray) {
                                if (s.toLowerCase().contains(AMOUNT_EQUAL_TO)) {
                                    String amountArray[] = s.split("=");
                                    if (amountArray.length > 1) {
                                        Toast.makeText(this, "Amount: " + amountArray[1],Toast.LENGTH_LONG).show();
                                        amount = amountArray[1];
                                    }
                                }
                            }
                        }
                        else {
                            // incorrect uri
                        }
                    }
                    else {
                        addr = contents;
                    }

                    if (addr.length() > 0) {
                        TextInputEditText inputAddress = findViewById(
                                R.id.input_edit_contact_address);
                        inputAddress.setText(addr);
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                // Handle cancel
                Toast.makeText(this, getString(R.string.qr_code_scan_cancelled),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveContact() {
        String newContactAddress = mInputContactAddress.getText().toString();
        String newContactName = mInputContactName.getText().toString();
        DBAddrItem newItem = new DBAddrItem(newContactAddress, newContactName);

        if ((newContactAddress != mContactAddress) || (newContactName != mContactName)) {
            if (newContactAddress != mContactAddress) {
                mOtkDB.deleteAddressbookByAddr(mContactAddress);
                if (!newContactAddress.isEmpty()) {
                    mOtkDB.addAddress(newItem);
                }
            }
            else {
                mOtkDB.updateAddressbook(newItem);
            }
        }
        finishActivity();
    }

    private void deleteContact() {
        mOtkDB.deleteAddressbookByAddr(mContactAddress);
        finishActivity();
    }

    private void finishActivity() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private void launchQRcodeScanActivity(View v) {
        Log.d(TAG, "launchQRcodeScanActivity");
        if (ContextCompat.checkSelfPermission(EditContactActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent();
            intent.setClass(EditContactActivity.this, QRcodeScanActivity.class);
            this.startActivityForResult(intent, MainActivity.REQUEST_CODE_QR_CODE);
        }
    }

}
