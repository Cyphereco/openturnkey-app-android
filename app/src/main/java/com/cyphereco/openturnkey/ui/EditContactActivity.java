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

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;

import java.util.List;

public class EditContactActivity extends AppCompatActivity {
    private final static String TAG = AddContactActivity.class.getSimpleName();
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
                finish();
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

        List<DBAddrItem> addrDataset = mOtkDB.getAllAddressbook();
        for (int i = 0; i < addrDataset.size(); i++) {
            DBAddrItem item = addrDataset.get(i);
            Log.d(TAG, "name: " + item.getName() + ", address: " + item.getAddress());
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
        finish();
    }

    private void deleteContact() {
        mOtkDB.deleteAddressbookByAddr(mContactAddress);
        finish();
    }

    public void launchQRcodeScanActivity(View v) {
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
