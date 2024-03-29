package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.db.RecordAddress;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.AddressUtils;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.util.Objects;

public class ActivityAddressEditor extends ActivityExtendOtkNfcReader {
    private final static String TAG = ActivityAddressEditor.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    public static final String KEY_QR_CODE = "KEY_QR_CODE";
    public static final String KEY_EDITOR_CONTACT_DB_ID = "KEY_EDITOR_CONTACT_DB_ID";
    public static final String KEY_EDITOR_CONTACT_ALIAS = "KEY_EDITOR_CONTACT_ALIAS";
    public static final String KEY_EDITOR_CONTACT_ADDR = "KEY_EDITOR_CONTACT_ADDR";
    public static final long DEFAULT_DB_ID = 0; // If this is selected, this editor is for add

    private static String mAlias;
    private static String mAddress;

    private TextInputEditText mInputAlias;
    private TextInputEditText mInputAddress;
    private ImageView mQRCodeScanBtn;
    private ImageView mPasteClipboardBtn;
    private ImageView mReadNFCBtn;
    private Button mSaveBtn;

    private long mAddrDBId = DEFAULT_DB_ID;

    @RequiresApi(api = Build.VERSION_CODES.O)
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
        if (Build.VERSION.SDK_INT >= 26)    // setTooltipText requires Android SDK API level 26 above
           mPasteClipboardBtn.setTooltipText(getString(R.string.paste_clipboard));

        mQRCodeScanBtn = findViewById(R.id.icon_scan_qrcode_edit_addr);
        if (Build.VERSION.SDK_INT >= 26)    // setTooltipText requires Android SDK API level 26 above
            mQRCodeScanBtn.setTooltipText(getString(R.string.scan_qr_code));

        mReadNFCBtn = findViewById(R.id.icon_read_nfc_edit_addr);
        if (Build.VERSION.SDK_INT >= 26)    // setTooltipText requires Android SDK API level 26 above
            mReadNFCBtn.setTooltipText(getString(R.string.read_nfc));

        mSaveBtn = findViewById(R.id.button_save_edit_addr);

        Intent intent = this.getIntent();

        // retrieve passed in parameter when editing address
        mAddrDBId = intent.getLongExtra(KEY_EDITOR_CONTACT_DB_ID, 0);

        if (null != intent.getStringExtra(KEY_EDITOR_CONTACT_ALIAS)) {
            mInputAlias.setText(intent.getStringExtra(KEY_EDITOR_CONTACT_ALIAS));
        }
        if (null != intent.getStringExtra(KEY_EDITOR_CONTACT_ADDR)) {
            mInputAddress.setText(intent.getStringExtra(KEY_EDITOR_CONTACT_ADDR));
        }

        // enable/disable 'save' button according to whether address field is empty
        if ((null != mInputAddress.getText()) && (!mInputAddress.getText().toString().isEmpty())) {
            mSaveBtn.setEnabled(true);
        }

        setUIListener();
    }

    @Override
    public void onOtkDataPosted(OtkData data) {
        super.onOtkDataPosted(data);
        if (data != null) {
            logger.debug("got otkData: {}", otkData.toString());
            String addr = data.getSessionData().getAddress();

            if (BtcUtils.validateAddress(!Preferences.isTestnet(), addr)) {
                mAddress = addr;
                mInputAddress.setText(mAddress);
            } else {
                AlertPrompt.alert(this, getString(R.string.invalid_address));
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MainActivity.setCurrentActivity(MainActivity.class.getName());
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        logger.debug("resumed");

        // set current activity in MainActivity property
        MainActivity.setCurrentActivity(getClass().getName());

        // try to restore previous stored inputs
        if (mAlias != null) mInputAlias.setText(mAlias);
        if (mAddress != null) mInputAddress.setText(mAddress);
    }

    @Override
    protected void onPause() {
        super.onPause();
        logger.debug("paused");

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                assert data != null;
                String addr = data.getStringExtra(KEY_QR_CODE);
                logger.info("QR result: {}", addr);

                addr = addr.replace("bitcoin:","");
                if (BtcUtils.validateAddress(!Preferences.isTestnet(), addr)) {
                    mAddress = addr;
                    mInputAddress.setText(mAddress.replace("bitcoin:", ""));
                } else {
                    if (BtcUtils.isSegWitAddress(!Preferences.isTestnet(), addr)) {
                        AlertPrompt.alert(this, getString(R.string.seg_wit_address_is_not_supported));
                    } else {
                        AlertPrompt.alert(this, getString(R.string.invalid_address));
                    }
                }
            }
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

                        if (BtcUtils.validateAddress(!Preferences.isTestnet(), addr)) {
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
                showDialogReadOtk(null, null);
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
            RecordAddress item = new RecordAddress(address, alias);
            RecordAddress existItem = OpenturnkeyDB.getAddressByAlias(alias);
            if (mAddrDBId > 0) {
                if ((existItem != null) && (existItem.getId() != mAddrDBId)) {
                    // Show dialog
                    showAliasDuplicateDialog();
                    return;
                }
                // Update database
                item.setId(mAddrDBId);
                if (!OpenturnkeyDB.updateAddressbook(item)) {
                    AlertPrompt.alert(this, getString(R.string.update_addr_fail));
                }
            } else {
                if (existItem != null) {
                    // Show dialog
                    showAliasDuplicateDialog();
                    return;
                }
                if (OpenturnkeyDB.insertAddress(item) == null) {
                    AlertPrompt.alert(this, getString(R.string.add_new_addr_fail));
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
        new AlertDialog.Builder(this, R.style.AlertDialogNarrowWidth)
                .setMessage(R.string.alias_exists)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
}
