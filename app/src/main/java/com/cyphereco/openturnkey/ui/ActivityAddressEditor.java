package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Otk;
import com.cyphereco.openturnkey.core.OtkEvent;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.AddressUtils;

import java.util.Objects;

public class ActivityAddressEditor extends AppCompatActivity {
    private final static String TAG = ActivityAddressEditor.class.getSimpleName();

    private final static int ZXING_CAMERA_PERMISSION = 1;
    private static final String BEGIN_BITCOIN_SIGNED_MESSAGE = "-----BEGIN BITCOIN SIGNED MESSAGE-----";
    private static final String AMOUNT_EQUAL_TO = "amount=";

    public static final String KEY_QR_CODE = "KEY_QR_CODE";
    public static final String KEY_EDITOR_TYPE = "KEY_EDITOR_TYPE";
    public static final String KEY_EDITOR_CONTACT_ALIAS = "KEY_EDITOR_CONTACT_ALIAS";
    public static final String KEY_EDITOR_CONTACT_ADDR = "KEY_EDITOR_CONTACT_ADDR";
    public static final int EDITOR_TYPE_ADD = 1;
    public static final int EDITOR_TYPE_EDIT = 2;

    private OpenturnkeyDB mOtkDB;
    private TextInputEditText mInputAlias;
    private TextInputEditText mInputAddress;
    private ImageView mQRCodeScanBtn;
    private ImageView mPasteClipboardBtn;
    private ImageView mReadNFCBtn;
    private Button mSaveBtn;
    private ProgressBar mReadNFCProgressBar;

    private NfcAdapter mNfcAdapter = null;
    private static Otk mOtk = null;
    private static boolean mWaitingNFC = false;


    static public boolean isActive() {
        return (mOtk != null);
    }

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
        mReadNFCProgressBar = findViewById(R.id.progressBar_read_nfc_edit_addr);

        mOtkDB = new OpenturnkeyDB(getApplicationContext());

        Intent intent = this.getIntent();
        if (EDITOR_TYPE_EDIT == intent.getIntExtra(KEY_EDITOR_TYPE, 0)) {
            mInputAlias.setText(intent.getStringExtra(KEY_EDITOR_CONTACT_ALIAS));
            mInputAddress.setText(intent.getStringExtra(KEY_EDITOR_CONTACT_ADDR));
        }

        setUIListener();

        /* init NFC. */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mOtk = Otk.getInstance(getApplicationContext());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            if (mOtk == null) {
                Log.d(TAG, "mOtk is null");
                return;
            }
            if (!mWaitingNFC) {
                Log.d(TAG, "NFC processing is disable");
                return;
            }
            int ret = mOtk.processIntent(intent, new Otk.OtkEventListener() {
                @Override
                public void onOtkEvent(OtkEvent event) {
                    Log.d(TAG, "onOtkEvent");
                    if (event.getType() != OtkEvent.Type.GENERAL_INFORMATION) {
                        return;
                    }
                    // Update Address info
                    mInputAddress.setText(event.getData().getSessionData().getAddress());
                }
            });
            if (ret != Otk.OTK_RETURN_OK) {
                Log.d(TAG, "process intent failed:" + ret);
            }

            disableReadNFCProcessing();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter =
                new IntentFilter[] {techDetected, tagDetected, ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(
                        Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent,
                    nfcIntentFilter, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mOtk == null) {
            Log.d(TAG, "mOtk is null");
            return;
        }
        mOtk = null;
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Setup onTouchEvent for detecting type of touch gesture
        disableReadNFCProcessing();
        return super.dispatchTouchEvent(event);
    }

    private void setUIListener() {
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContact();
            }
        });

        mPasteClipboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(
                        Context.CLIPBOARD_SERVICE);
                if (clipboard.hasPrimaryClip()) {
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
                // Waiting NFC scan and process it.
                mWaitingNFC = true;
                mReadNFCProgressBar.setVisibility(View.VISIBLE);
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
        Log.d(TAG, "onActivityResult:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                // Handle successful scan
                String contents = intent.getStringExtra(KEY_QR_CODE);
                if (contents.contains(BEGIN_BITCOIN_SIGNED_MESSAGE)) {
                    Log.d(TAG, "Contents contains BEGIN_BITCOIN_SIGNED_MESSAGE");
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
                            Log.d(TAG, "Incorrect URI format");
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
                            Log.d(TAG, "Incorrect URI format");
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

    private void disableReadNFCProcessing() {
        if (mReadNFCProgressBar.getVisibility() == View.VISIBLE) {
            mReadNFCProgressBar.setVisibility(View.INVISIBLE);
            // Disable NFC process
            mWaitingNFC = false;
        }
    }

    private void saveContact() {
        String alias = Objects.requireNonNull(mInputAlias.getText()).toString();
        String address = Objects.requireNonNull(mInputAddress.getText()).toString();

        if (alias.isEmpty()) {
            alias = AddressUtils.getShortAddress(address);
            mInputAlias.setText(alias);
        }

        if ((!alias.isEmpty()) && (!address.isEmpty())) {
            Log.d(TAG, "Run save contact alias: " + alias + ", address: " + address);
            DBAddrItem item = new DBAddrItem(address, alias);
            // Check this contact if already in database
            if (null != mOtkDB.getAddressItemByAddr(address)) {
                // update address information
                if (!mOtkDB.updateAddressbook(item)) {
                    Log.e(TAG, "Update address failed");
                }
            }
            else {
                // Add new address
                if (null == mOtkDB.addAddress(item)) {
                    Log.e(TAG, "Add address failed");
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

    private void launchQRcodeScanActivity() {
        Log.d(TAG, "launchQRcodeScanActivity");
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

}
