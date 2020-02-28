package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.AddressUtils;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.util.Objects;

public class ActivityAddressEditor extends AppCompatActivity {
    private final static String TAG = ActivityAddressEditor.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private final static int ZXING_CAMERA_PERMISSION = 1;
    private static final String BEGIN_BITCOIN_SIGNED_MESSAGE = "-----BEGIN BITCOIN SIGNED MESSAGE-----";
    private static final String AMOUNT_EQUAL_TO = "amount=";

    public static final String KEY_QR_CODE = "KEY_QR_CODE";
    public static final String KEY_EDITOR_CONTACT_DB_ID = "KEY_EDITOR_CONTACT_DB_ID";
    public static final String KEY_EDITOR_CONTACT_ALIAS = "KEY_EDITOR_CONTACT_ALIAS";
    public static final String KEY_EDITOR_CONTACT_ADDR = "KEY_EDITOR_CONTACT_ADDR";
    public static final long DEFAULT_DB_ID = 0; // If this is selected, this editor is for add

    private OpenturnkeyDB mOtkDB;
    private TextInputEditText mInputAlias;
    private TextInputEditText mInputAddress;
    private ImageView mQRCodeScanBtn;
    private ImageView mPasteClipboardBtn;
    private ImageView mReadNFCBtn;
    private Button mSaveBtn;

    private long mAddrDBId = DEFAULT_DB_ID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_editor);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = findViewById(R.id.toolbar_addr_editor);
        setSupportActionBar(toolbar);
        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mInputAlias = findViewById(R.id.input_contact_alias);
        mInputAddress = findViewById(R.id.input_contact_address);
        mPasteClipboardBtn = findViewById(R.id.icon_paste_clipboard_edit_addr);
        mQRCodeScanBtn = findViewById(R.id.icon_scan_qrcode_edit_addr);
        mReadNFCBtn = findViewById(R.id.icon_read_nfc_edit_addr);
        mSaveBtn = findViewById(R.id.button_save_edit_addr);

        mOtkDB = new OpenturnkeyDB(getApplicationContext());

        Intent intent = this.getIntent();
        mAddrDBId = intent.getLongExtra(KEY_EDITOR_CONTACT_DB_ID, 0);
        logger.debug("mAddrDBId: " + mAddrDBId);
        if (null != intent.getStringExtra(KEY_EDITOR_CONTACT_ALIAS)) {
            mInputAlias.setText(intent.getStringExtra(KEY_EDITOR_CONTACT_ALIAS));
        }
        if (null != intent.getStringExtra(KEY_EDITOR_CONTACT_ADDR)) {
            mInputAddress.setText(intent.getStringExtra(KEY_EDITOR_CONTACT_ADDR));
        }

        if ((null != mInputAddress.getText()) && (!mInputAddress.getText().toString().isEmpty())) {
            mSaveBtn.setEnabled(true);
        }

        setUIListener();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            onBackPressed();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void setUIListener() {
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContact(v);
            }
        });

        mPasteClipboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(
                        Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    android.content.ClipDescription description =
                            clipboard.getPrimaryClipDescription();
                    android.content.ClipData data = clipboard.getPrimaryClip();
                    if (data != null && description != null) {
                        String address = data.getItemAt(0).getText().toString();
                        mInputAddress.setText(address);
                    }
                }
            }
        });

        mReadNFCBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivityToLaunchNFCReader();
            }
        });

        mQRCodeScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchQRcodeScanActivity();
            }
        });

        mInputAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if ((null != mInputAddress.getText()) &&
                        (!mInputAddress.getText().toString().isEmpty())) {
                    mSaveBtn.setEnabled(true);
                }
                else {
                    mSaveBtn.setEnabled(false);
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logger.debug("onActivityResult:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                // Handle successful scan
                String contents = intent.getStringExtra(KEY_QR_CODE);
                if (contents.contains(BEGIN_BITCOIN_SIGNED_MESSAGE)) {
                    logger.debug("Contents contains BEGIN_BITCOIN_SIGNED_MESSAGE");
                    // TODO
                    // updateFormattedSignedMessage(contents);
                }
                else {
                    String addr = "";
                    boolean notBTC = false;

                    if (contents.contains(":")) {
                        // contents might be a uri
                        String[] uriArray = contents.split(":");

                        if (uriArray.length > 1) {
                            if (uriArray[0].contentEquals("bitcoin")) {
                                contents = uriArray[1];
                                addr = contents;
                            }
                            else {
                                notBTC = true;
                                Toast.makeText(this, uriArray[0] +
                                        getString(R.string.not_supported),
                                        Toast.LENGTH_LONG).show();
                                contents = "";
                            }
                        }
                        else {
                            // incorrect uri format
                            logger.debug("Incorrect URI format");
                        }
                    }

                    if (!notBTC && contents.contains("?")) {
                        // contents might contains query tag
                        String[] queryArray = contents.split("\\?");

                        if (queryArray.length > 1) {
                            addr = queryArray[0];

                            String[] queryTagArray = queryArray[1].split("&");

                            for (String s : queryTagArray) {
                                if (s.toLowerCase().contains(AMOUNT_EQUAL_TO)) {
                                    String[] amountArray = s.split("=");
                                    if (amountArray.length > 1) {
                                        Toast.makeText(this,
                                                getString(R.string.subject_amount)+ amountArray[1],
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        }
                        else {
                            // incorrect uri
                            logger.debug("Incorrect URI format");
                        }
                    }
                    else {
                        addr = contents;
                    }

                    if (addr.length() > 0) {
                        mInputAddress.setText(addr);
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                // Handle cancel
                Toast.makeText(this, getString(R.string.qr_code_scan_cancelled), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveContact(View v) {
        String alias = Objects.requireNonNull(mInputAlias.getText()).toString();
        String address = Objects.requireNonNull(mInputAddress.getText()).toString();

        try {
            // Check if address is valid
            if (address.length() == 0) {
                Snackbar.make(v, getString(R.string.recipient_is_empty), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                return;
            }
            if (BtcUtils.isSegWitAddress(!Preferences.isTestnet(getApplicationContext()), address)) {
                Snackbar.make(v, getString(R.string.seg_wit_address_is_not_supported), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                return;
            }
            if (!BtcUtils.validateAddress(!Preferences.isTestnet(getApplicationContext()), address)) {
                Snackbar.make(v, getString(R.string.invalid_address), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                return;
            }
        }
        catch (NullPointerException | NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        if (alias.isEmpty()) {
            alias = AddressUtils.getShortAddress(address);
            mInputAlias.setText(alias);
        }

        if (!alias.isEmpty()) {
            DBAddrItem item = new DBAddrItem(address, alias);
            DBAddrItem existItem = mOtkDB.getAddressItemByAlias(alias);
            if (mAddrDBId > 0) {
                if ((existItem != null) && (existItem.getDbId() != mAddrDBId)) {
                    // Show dialog
                    showAliasDuplicateDialog();
                    return;
                }
                // Update database
                item.setDbId(mAddrDBId);
                if (!mOtkDB.updateAddressbook(item)) {
                    logger.error("Update address failed");
                }
            }
            else {
                if (existItem != null) {
                    // Show dialog
                    showAliasDuplicateDialog();
                    return;
                }
                if (!mOtkDB.addAddress(item)) {
                    logger.error("Add address failed");
                }
            }
        }

        finishActivity();
    }

    private void finishActivity() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private void finishActivityToLaunchNFCReader() {
        Intent intent = new Intent();
        if (null == mInputAlias.getText()) {
            intent.putExtra(MainActivity.KEY_ADDRESS_EDITOR_TEMP_ALIAS, "");
        }
        else {
            intent.putExtra(MainActivity.KEY_ADDRESS_EDITOR_TEMP_ALIAS,
                    mInputAlias.getText().toString());
        }

        if (null == mInputAddress.getText()) {
            intent.putExtra(MainActivity.KEY_ADDRESS_EDITOR_TEMP_ADDR, "");
        }
        else {
            intent.putExtra(MainActivity.KEY_ADDRESS_EDITOR_TEMP_ADDR,
                    mInputAddress.getText().toString());
        }
        intent.putExtra(KEY_EDITOR_CONTACT_DB_ID, mAddrDBId);
        setResult(MainActivity.REQUEST_RESULT_CODE_READ_NFC, intent);
        finish();
    }

    private void launchQRcodeScanActivity() {
        if (ContextCompat.checkSelfPermission(ActivityAddressEditor.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent();
            intent.setClass(ActivityAddressEditor.this, QRcodeScanActivity.class);
            this.startActivityForResult(intent, MainActivity.REQUEST_CODE_QR_CODE);
        }
    }

    private void showAliasDuplicateDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Alias exist already!!!")
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
}
