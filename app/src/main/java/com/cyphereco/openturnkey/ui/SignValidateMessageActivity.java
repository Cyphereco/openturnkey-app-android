package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.QRCodeUtils;
import com.cyphereco.openturnkey.utils.SignedMessage;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Objects;

public class SignValidateMessageActivity extends AppCompatActivity {
    public static final String TAG = SignValidateMessageActivity.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);

    private NfcAdapter mNfcAdapter = null;

    private ViewPager mViewPager;

    private String mFormattedSignedMsg;
    private String mMsgToSign;

    private static final int ZXING_CAMERA_PERMISSION = 1;
    public static final int REQUEST_CODE_QR_CODE = 0;
    private boolean mUsingMasterKey;

    public void launchQRcodeScanActivity(View v) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent(getApplicationContext(), QRcodeScanActivity.class);
            startActivityForResult(intent, SignValidateMessageActivity.REQUEST_CODE_QR_CODE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logger.info("onActivityResult:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                // Handle successful scan
                String contents = intent.getStringExtra(MainActivity.KEY_QR_CODE);
                EditText etMsgToBeVerified = findViewById(R.id.editTextMessageToBeVerified);
                etMsgToBeVerified.setText(contents);
            } else if (resultCode == RESULT_CANCELED) {
                //Handle cancel
                Toast.makeText(this, getString(R.string.qr_code_scan_cancelled), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate");
        setContentView(R.layout.activity_sign_validate_message);

        Toolbar toolbar = findViewById(R.id.toolbar_sign_verify_message);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);

        TabLayout mTabs = findViewById(R.id.tabLayoutSignVerify);
        mTabs.bringToFront();

        mViewPager = findViewById(R.id.viewPagerSignVerify);
        mViewPager.setAdapter(new SignVerifyPagerAdapter());
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabs));

        mTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        final OtkData otkData = (OtkData) intent.getSerializableExtra(MainActivity.KEY_OTK_DATA);
        if (otkData == null) {
            return;
        }

        mMsgToSign = (String) intent.getSerializableExtra(MainActivity.KEY_MESSAGE_TO_SIGN);
        mUsingMasterKey = intent.getBooleanExtra(MainActivity.KEY_USING_MASTER_KEY, false);
        String pubKey = otkData.getSessionData().getPublicKey();
        logger.info("message to sign:{} pubKey:{} signature:{}", mMsgToSign, pubKey, otkData.getSessionData().getRequestSigList().get(0));
        try {
            String signedMsg = BtcUtils.processSignedMessage(
                    BtcUtils.generateMessageToSign(mMsgToSign),
                    BtcUtils.hexStringToBytes(pubKey),
                    BtcUtils.hexStringToBytes(otkData.getSessionData().getRequestSigList().get(0)));
            String publicAddress = BtcUtils.keyToAddress(pubKey);
            SignedMessage sm = new SignedMessage(publicAddress, signedMsg, mMsgToSign);
            mFormattedSignedMsg = sm.getFormattedMessage();
        } catch (Exception e) {
            // Failed to process signed message
            Toast.makeText(getApplicationContext(), R.string.sign_message_fail, Toast.LENGTH_LONG).show();
        }

        /* init NFC. */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    class SignVerifyPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NotNull View view, @NotNull Object obj) {
            return obj == view;
        }

        private void updateSignMessageButton(View view, String msgToSign) {
            Button btn = view.findViewById(R.id.buttonSignMessage);
            if (msgToSign.length() > 0) {
                btn.setEnabled(true);
                btn.setAlpha(1.0f);
            } else {
                btn.setEnabled(false);
                btn.setAlpha(.5f);
            }
        }

        private void updateVerifyMessageButton(View view, String msgToSign) {
            Button btn = view.findViewById(R.id.buttonVerifyMessage);
            if (msgToSign.length() > 0) {
                btn.setEnabled(true);
                btn.setAlpha(1.0f);
            } else {
                btn.setEnabled(false);
                btn.setAlpha(.5f);
            }
        }

        private void initSignMessageTab(final View view) {
            if (mFormattedSignedMsg != null && mFormattedSignedMsg.length() > 0) {
                ImageView ivQr = view.findViewById(R.id.signMessageGenerateQRcode);
                ivQr.setEnabled(true);
                ivQr.setVisibility(View.VISIBLE);
                ivQr.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ImageView image = new ImageView(getApplicationContext());
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int size = displayMetrics.widthPixels * 7 / 10;
                        Bitmap bitmap = QRCodeUtils.encodeAsBitmap(mFormattedSignedMsg, size, size);
                        image.setImageBitmap(bitmap);

                        AlertDialog.Builder builder = new AlertDialog.Builder(SignValidateMessageActivity.this)
                                .setCancelable(true)
                                .setView(image);
                        builder.create().show();
                    }
                });
                ImageView ivCopy = view.findViewById(R.id.signMessageCopy);
                ivCopy.setEnabled(true);
                ivCopy.setVisibility(View.VISIBLE);
                ivCopy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Signed message", mFormattedSignedMsg);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                        }
                        Toast.makeText(getApplicationContext(), R.string.data_copied, Toast.LENGTH_LONG).show();
                    }
                });
                EditText etSignedMsg = view.findViewById(R.id.editTextSignedMessage);
                etSignedMsg.setEnabled(true);
                etSignedMsg.setVisibility(View.VISIBLE);
                etSignedMsg.setText(mFormattedSignedMsg);
                TextView tvSignedMsg = view.findViewById(R.id.textViewSignedMessage);
                tvSignedMsg.setEnabled(true);
                tvSignedMsg.setVisibility(View.VISIBLE);
            } else {
                ImageView ivQr = view.findViewById(R.id.signMessageGenerateQRcode);
                ivQr.setEnabled(false);
                ivQr.setVisibility(View.INVISIBLE);
                ImageView ivCopy = view.findViewById(R.id.signMessageCopy);
                ivCopy.setEnabled(false);
                ivCopy.setVisibility(View.INVISIBLE);
                EditText etSignedMsg = view.findViewById(R.id.editTextSignedMessage);
                etSignedMsg.setEnabled(false);
                etSignedMsg.setVisibility(View.INVISIBLE);
                TextView tvSignedMsg = view.findViewById(R.id.textViewSignedMessage);
                tvSignedMsg.setEnabled(false);
                tvSignedMsg.setVisibility(View.INVISIBLE);
            }
            EditText etMsgToSign = view.findViewById(R.id.editTextMessageToBeSign);
            if (mMsgToSign != null && mMsgToSign.length() > 0) {
                etMsgToSign.setText(mMsgToSign);
            }

            CheckBox cbUsingMasterKey = view.findViewById(R.id.checkBoxUsingMasterKey);
            cbUsingMasterKey.setChecked(mUsingMasterKey);

            updateSignMessageButton(view, etMsgToSign.getText().toString());
            etMsgToSign.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable arg0) {
                    updateSignMessageButton(view, arg0.toString());
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            Button btnSignMsg = view.findViewById(R.id.buttonSignMessage);
            btnSignMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Using Master Key checkbox
                    CheckBox cbUsingMasterKey = view.findViewById(R.id.checkBoxUsingMasterKey);
                    EditText etMsgToSign = view.findViewById(R.id.editTextMessageToBeSign);
                    String msgToSign = etMsgToSign.getText().toString();
                    logger.debug("Message to sign:{}, using master key:{}", msgToSign, cbUsingMasterKey.isChecked());
                    Intent intent = new Intent();
                    intent.putExtra(MainActivity.KEY_SIGN_VALIDATE_MESSAGE, msgToSign);
                    intent.putExtra(MainActivity.KEY_USING_MASTER_KEY, cbUsingMasterKey.isChecked());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }

        private void initVerifyMessageTab(final View view) {
            final EditText etMsgToBeVerified = view.findViewById(R.id.editTextMessageToBeVerified);

            ImageView ivGreen = findViewById(R.id.imageViewVerifyGreen);
            ImageView ivFail = findViewById(R.id.imageViewVerifyFail);
            ivGreen.setVisibility(View.INVISIBLE);
            ivFail.setVisibility(View.INVISIBLE);

            ImageView ivPaste = findViewById(R.id.imageViewVerifyPaste);
            ivPaste.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = null;
                    if (clipboard != null) {
                        clip = clipboard.getPrimaryClip();
                    }
                    try {
                        if (clip != null) {
                            etMsgToBeVerified.setText(clip.getItemAt(0).getText());
                        }
                    } catch (Exception e) {
                        // Nothing to be pasted
                    }
                }
            });
            ImageView ivScanQR = findViewById(R.id.imageViewVerifyScanQR);
            ivScanQR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchQRcodeScanActivity(view);
                }
            });

            updateVerifyMessageButton(view, etMsgToBeVerified.getText().toString());
            etMsgToBeVerified.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable arg0) {
                    updateVerifyMessageButton(view, arg0.toString());
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            Button btnVerifyMsg = view.findViewById(R.id.buttonVerifyMessage);
            btnVerifyMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String msgToBeVerified = etMsgToBeVerified.getText().toString();
                    SignedMessage sm = SignedMessage.parseSignedMessage(msgToBeVerified);
                    ImageView ivGreen = findViewById(R.id.imageViewVerifyGreen);
                    ImageView ivFail = findViewById(R.id.imageViewVerifyFail);

                    if (sm != null) {
                        boolean isVerified = BtcUtils.verifySignature(sm.getAddress(), sm.getMessage(), sm.getSignature());
                        if (isVerified) {
                            ivGreen.setVisibility(View.VISIBLE);
                            ivFail.setVisibility(View.INVISIBLE);
                        } else {
                            ivGreen.setVisibility(View.INVISIBLE);
                            ivFail.setVisibility(View.VISIBLE);
                        }
                    } else {
                        ivGreen.setVisibility(View.INVISIBLE);
                        ivFail.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        @NotNull
        @Override
        public Object instantiateItem(@NotNull ViewGroup container, int position) {
            View view;
            if (position == 0) {
                view = getLayoutInflater().inflate(R.layout.layout_sign_message, container, false);
                container.addView(view);
                initSignMessageTab(view);
            } else {
                view = getLayoutInflater().inflate(R.layout.layout_verify_message, container, false);
                container.addView(view);
                initVerifyMessageTab(view);
            }
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NotNull Object object) {
            container.removeView((View) object);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected, tagDetected, ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }
}
