package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.db.DBAddrItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.AddressUtils;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.core.NfcHandler;

import org.slf4j.Logger;

import java.util.Objects;

public class ActivityAddressEditor extends AppCompatActivity {
    private final static String TAG = ActivityAddressEditor.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    public static final String KEY_QR_CODE = "KEY_QR_CODE";
    public static final String KEY_EDITOR_CONTACT_DB_ID = "KEY_EDITOR_CONTACT_DB_ID";
    public static final String KEY_EDITOR_CONTACT_ALIAS = "KEY_EDITOR_CONTACT_ALIAS";
    public static final String KEY_EDITOR_CONTACT_ADDR = "KEY_EDITOR_CONTACT_ADDR";
    public static final long DEFAULT_DB_ID = 0; // If this is selected, this editor is for add

    private static OtkData mOtkData;
    private static String mAlias;
    private static String mAddress;

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
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void setUIListener() {
        final Context ctx = this;
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
                        String addr = data.getItemAt(0).getText().toString();

                        if (MainActivity.isAddressValid(addr)) {
                            mAddress = addr;
                            mInputAddress.setText(mAddress);
                        } else {
                            AlertPrompt.alert(ctx, getString(R.string.invalid_address));
                        }
                    }
                }
            }
        });

        mReadNFCBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogReadOtk dialogReadOtk = new DialogReadOtk();
                FragmentManager fm = getSupportFragmentManager();
                dialogReadOtk.show(fm, "Get Address for Address Editor");
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
                } else {
                    mSaveBtn.setEnabled(false);
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logger.debug("onActivityResult:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                String addr = intent.getStringExtra(KEY_QR_CODE);
                logger.info("QR result: {}", addr);

                if (MainActivity.isAddressValid(addr)) {
                    mAddress = addr;
                    mInputAddress.setText(mAddress);
                } else {
                    AlertPrompt.alert(this, getString(R.string.invalid_address));
                }
            }
        }
    }

    private void saveContact(View v) {
        String alias = Objects.requireNonNull(mInputAlias.getText()).toString();
        String address = Objects.requireNonNull(mInputAddress.getText()).toString();

        try {
            // Check if address is valid
            if (address.length() == 0) {
                AlertPrompt.alert(v.getContext(), getString(R.string.recipient_is_empty));
                return;
            }
            if (BtcUtils.isSegWitAddress(!Preferences.isTestnet(), address)) {
                AlertPrompt.alert(v.getContext(), getString(R.string.seg_wit_address_is_not_supported));
                return;
            }
            if (!BtcUtils.validateAddress(!Preferences.isTestnet(), address)) {
                AlertPrompt.alert(v.getContext(), getString(R.string.invalid_address));
                return;
            }
        } catch (NullPointerException | NumberFormatException e) {
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
            } else {
                if (existItem != null) {
                    // Show dialog
                    showAliasDuplicateDialog();
                    return;
                }
                if (!mOtkDB.addAddress(item)) {
                    logger.error("Add address failed");
                }
            }
            finish();
        }
    }

    private void launchQRcodeScanActivity() {
        Intent intent = new Intent(this, ActivityQRcodeScan.class);
        startActivityForResult(intent, MainActivity.REQUEST_CODE_QR_CODE);
    }

    private void showAliasDuplicateDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.alias_existed)
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

        // set current activity in MainActivity property
        MainActivity.setCurrentActivity(getClass().getName());

        // try to restore previous stored inputs
        if (mAlias != null) mInputAlias.setText(mAlias);
        if (mAddress != null) mInputAddress.setText(mAddress);
        if (mOtkData != null) {
            String addr = mOtkData.getSessionData().getAddress();
            mOtkData = null;

            if (MainActivity.isAddressValid(addr)) {
                mAddress = addr;
                mInputAddress.setText(mAddress);
            } else {
                AlertPrompt.alert(this, getString(R.string.invalid_address));
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            OtkData otkData = NfcHandler.parseIntent(intent);

            if (otkData != null) {
                mOtkData = otkData;
                DialogReadOtk.updateReadOtkStatus(DialogReadOtk.READ_SUCCESS);
            } else {
                DialogReadOtk.updateReadOtkStatus(DialogReadOtk.NOT_OPENTURNKEY);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // store inputs in activity's properties
        mAlias = Objects.requireNonNull(mInputAlias.getText()).toString();
        mAddress = Objects.requireNonNull(mInputAddress.getText()).toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // reset activity's properties
        mAlias = null;
        mAddress = null;
        mOtkData = null;
    }
}
