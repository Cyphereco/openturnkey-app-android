package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkRequest;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.QRCodeUtils;
import com.cyphereco.openturnkey.utils.SignedMessage;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Objects;

public class ActivitySignValidateMessage extends ActivityExtendOtkNfcReader {
    public static final String TAG = ActivitySignValidateMessage.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);

    private ViewPager mViewPager;
    private SignVerifyPagerAdapter mPageAdapter;

    private String mFormattedSignedMsg;
    private String mMsgToSign;

    private static final int ZXING_CAMERA_PERMISSION = 1;
    public static final int REQUEST_CODE_QR_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_validate_message);

        Toolbar toolbar = findViewById(R.id.toolbar_sign_verify_message);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        TabLayout mTabs = findViewById(R.id.tabLayoutSignVerify);
        mTabs.bringToFront();

        mViewPager = findViewById(R.id.viewPagerSignVerify);
        mPageAdapter = new SignVerifyPagerAdapter();
        mViewPager.setAdapter(mPageAdapter);
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
    public void onBackPressed() {
        super.onBackPressed();
        // this is a workaround for getting back to MainActivity after app paused, hope there is a better solution
        startActivity(new Intent(this, ActivitySplash.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.setCurrentActivity(getClass().getName());
    }

    @Override
    public void onOtkDataPosted(OtkData data) {
        super.onOtkDataPosted(data);
        logger.debug("otkData: {}", data.toString());

        if (data.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_SUCCESS) {
            String pubKey = data.getSessionData().getPublicKey();
            try {
                String signedMsg = BtcUtils.processSignedMessage(
                        BtcUtils.generateMessageToSign(mMsgToSign),
                        BtcUtils.hexStringToBytes(pubKey),
                        BtcUtils.hexStringToBytes(data.getSessionData().getRequestSigList().get(0)));
                String publicAddress = BtcUtils.keyToAddress(pubKey);
                SignedMessage sm = new SignedMessage(publicAddress, signedMsg, mMsgToSign);
                mFormattedSignedMsg = sm.getFormattedMessage();
            } catch (Exception e) {
                // Failed to process signed message
                AlertPrompt.alert(getApplicationContext(), getString(R.string.sign_message_fail) +
                        "\n" + getString(R.string.reason) + ": " + parseFailureReason(null));
            }

            mPageAdapter.updateSignMessageOutput();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                // Handle successful scan
                assert data != null;
                String contents = data.getStringExtra(MainActivity.KEY_QR_CODE);
                EditText etMsgToBeVerified = findViewById(R.id.editTextMessageToBeVerified);
                etMsgToBeVerified.setText(contents);
            }
        }
    }

    public void launchQRcodeScanActivity(View v) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent(getApplicationContext(), ActivityQRcodeScan.class);
            startActivityForResult(intent, ActivitySignValidateMessage.REQUEST_CODE_QR_CODE);
        }
    }

    class SignVerifyPagerAdapter extends PagerAdapter {
        private View tabSignMessage;

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NotNull View view, @NotNull Object obj) {
            return obj == view;
        }

        void updateSignMessageOutput() {
            if (mFormattedSignedMsg != null && mFormattedSignedMsg.length() > 0) {
                ImageView ivQr = tabSignMessage.findViewById(R.id.signMessageGenerateQRcode);
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

                        AlertDialog.Builder builder = new AlertDialog.Builder(ActivitySignValidateMessage.this)
                                .setCancelable(true)
                                .setView(image);
                        builder.create().show();
                    }
                });
                ImageView ivCopy = tabSignMessage.findViewById(R.id.signMessageCopy);
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
                        AlertPrompt.info(getApplicationContext(), getString(R.string.data_copied));
                    }
                });
                EditText etSignedMsg = tabSignMessage.findViewById(R.id.editTextSignedMessage);
                etSignedMsg.setEnabled(true);
                etSignedMsg.setVisibility(View.VISIBLE);
                etSignedMsg.setText(mFormattedSignedMsg);
                TextView tvSignedMsg = tabSignMessage.findViewById(R.id.textViewSignedMessage);
                tvSignedMsg.setEnabled(true);
                tvSignedMsg.setVisibility(View.VISIBLE);
            } else {
                ImageView ivQr = tabSignMessage.findViewById(R.id.signMessageGenerateQRcode);
                ivQr.setEnabled(false);
                ivQr.setVisibility(View.INVISIBLE);
                ImageView ivCopy = tabSignMessage.findViewById(R.id.signMessageCopy);
                ivCopy.setEnabled(false);
                ivCopy.setVisibility(View.INVISIBLE);
                EditText etSignedMsg = tabSignMessage.findViewById(R.id.editTextSignedMessage);
                etSignedMsg.setEnabled(false);
                etSignedMsg.setVisibility(View.INVISIBLE);
                TextView tvSignedMsg = tabSignMessage.findViewById(R.id.textViewSignedMessage);
                tvSignedMsg.setEnabled(false);
                tvSignedMsg.setVisibility(View.INVISIBLE);
            }
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
            updateSignMessageOutput();

            EditText etMsgToSign = view.findViewById(R.id.editTextMessageToBeSign);
            if (mMsgToSign != null && mMsgToSign.length() > 0) {
                etMsgToSign.setText(mMsgToSign);
            }

            CheckBox cbUsingMasterKey = view.findViewById(R.id.checkBoxUsingMasterKey);
            cbUsingMasterKey.setChecked(false);

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

            final CheckBox cbUsePin = view.findViewById(R.id.sign_msg_use_pin);
            Button btnSignMsg = view.findViewById(R.id.buttonSignMessage);
            btnSignMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFormattedSignedMsg = "";
                    updateSignMessageOutput();
                    clearRequest();
                    if (cbUsePin.isChecked()) {
                        DialogAuthByPin dialogAuthByPin = new DialogAuthByPin();
                        dialogAuthByPin.setListener(new DialogAuthByPin.DialogAuthByPinListener() {
                            @Override
                            public void setPin(String pin) {
                                CheckBox cbUsingMasterKey = view.findViewById(R.id.checkBoxUsingMasterKey);
                                EditText etMsgToSign = view.findViewById(R.id.editTextMessageToBeSign);
                                mMsgToSign = etMsgToSign.getText().toString();
                                OtkRequest request = new OtkRequest(Command.SIGN.toString());
                                byte[] encodedMessageToSign = BtcUtils.generateMessageToSign(mMsgToSign);
                                assert encodedMessageToSign != null;
                                request.setData(BtcUtils.bytesToHexString(encodedMessageToSign));
                                if (cbUsingMasterKey.isChecked()) request.setMasterKey();
                                request.setPin(pin);
                                pushRequest(request);

                                DialogReadOtk dialogReadOtk = new DialogReadOtk();
                                dialogReadOtk.setOnCanelListener(new DialogReadOtk.dialogReadOtkListener() {
                                    @Override
                                    public void onCancel() {
                                        clearRequest();
                                    }
                                });
                                dialogReadOtk.show(getSupportFragmentManager(), "SIGN_MESSAGE");
                            }
                        });
                        dialogAuthByPin.show(getSupportFragmentManager(), "AuthByPin");
                    } else {
                        CheckBox cbUsingMasterKey = view.findViewById(R.id.checkBoxUsingMasterKey);
                        EditText etMsgToSign = view.findViewById(R.id.editTextMessageToBeSign);
                        mMsgToSign = etMsgToSign.getText().toString();
                        OtkRequest request = new OtkRequest(Command.SIGN.toString());
                        byte[] encodedMessageToSign = BtcUtils.generateMessageToSign(mMsgToSign);
                        assert encodedMessageToSign != null;
                        request.setData(BtcUtils.bytesToHexString(encodedMessageToSign));
                        if (cbUsingMasterKey.isChecked()) request.setMasterKey();
                        pushRequest(request);

                        DialogReadOtk dialogReadOtk = new DialogReadOtk();
                        dialogReadOtk.show(getSupportFragmentManager(), "SIGN_MESSAGE");
                    }
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
                            AlertPrompt.info(getApplicationContext(),getString(R.string.signature_valid));
                        } else {
                            ivGreen.setVisibility(View.INVISIBLE);
                            ivFail.setVisibility(View.VISIBLE);
                            AlertPrompt.alert(getApplicationContext(),getString(R.string.signature_invalid));
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
            if (position == 0) {
                tabSignMessage = getLayoutInflater().inflate(R.layout.layout_sign_message, container, false);
                container.addView(tabSignMessage);
                initSignMessageTab(tabSignMessage);
                return tabSignMessage;
            } else {
                View tabVerifyMessage = getLayoutInflater().inflate(R.layout.layout_verify_message, container, false);
                container.addView(tabVerifyMessage);
                initVerifyMessageTab(tabVerifyMessage);
                return tabVerifyMessage;
            }
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NotNull Object object) {
            container.removeView((View) object);
        }
    }

    public String parseFailureReason(String desc) {
        if (desc == null || desc.equals("")) {
            return getString(R.string.communication_error);
        }
        switch (desc) {
            case "C0":
                return getString(R.string.session_timeout);
            case "C1":
                return getString(R.string.auth_failed);
            case "C3":
                return getString(R.string.invalid_params);
            case "C4":
                return getString(R.string.missing_params);
            case "C7":
                return getString(R.string.pin_unset);
            case "00":
            case "C2":
            case "FF":
                return getString(R.string.invalid_command);
            default:
                return desc;
        }
    }
}
