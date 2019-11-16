package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

public class SignValidateMessageActivity extends AppCompatActivity {
    public static final String TAG = SignValidateMessageActivity.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);

    private TabLayout mTabs;
    private ViewPager mViewPager;

    private String mSignedMsg;

    private static final int ZXING_CAMERA_PERMISSION = 1;
    public static final int REQUEST_CODE_QR_CODE = 0;

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
                // TODO
            }
            else if (resultCode == RESULT_CANCELED) {
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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTabs = findViewById(R.id.tabLayoutSignVerify);

        mViewPager = (ViewPager) findViewById(R.id.viewPagerSignVerify);
        mViewPager.setAdapter(new SignVerifyPagerAdapter());
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabs));

        mTabs.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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

        final OtkData otkData = (OtkData)intent.getSerializableExtra(MainActivity.KEY_OTK_DATA);
        if (otkData == null) {
            return;
        }

        // TODO get msgToSign from OtkData
        String msgToSign = "";
        mSignedMsg = BtcUtils.processSignedMessage(
                BtcUtils.hexStringToBytes(msgToSign),
                BtcUtils.hexStringToBytes(otkData.getPublicKey()),
                BtcUtils.hexStringToBytes(otkData.getSessionData().getRequestSigList().get(0)));
    }

    class SignVerifyPagerAdapter extends PagerAdapter {
        private int pageCount = 2;
        @Override
        public int getCount() {
            return pageCount;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return obj == view;
        }

        private void updateSignMessageButton(View view, String msgToSign) {
            Button btn = view.findViewById(R.id.buttonSignMessage);
            if (msgToSign.length() > 0) {
                btn.setEnabled(true);
                btn.setAlpha(1.0f);
            }
            else {
                btn.setEnabled(false);
                btn.setAlpha(.5f);
            }
        }

        private void initSignMessageTab(final View view) {
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

            EditText etMsgToSign = view.findViewById(R.id.editTextMessageToBeSign);
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
                    EditText etMsgToSign = view.findViewById(R.id.editTextMessageToBeSign);
                    String msgToSign = etMsgToSign.getText().toString();
                    logger.debug("Message to sign:{}", msgToSign);
                    byte[] encodedMessageToSign = BtcUtils.generateMessageToSign(msgToSign);
                    Intent intent = new Intent();
                    intent.putExtra(MainActivity.KEY_SIGN_VALIDATE_MESSAGE, BtcUtils.bytesToHexString(encodedMessageToSign));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }

        private void initVerifyMessageTab(final View view) {
            //TODO
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = null;
            if (position == 0) {
                view = getLayoutInflater().inflate(R.layout.layout_sign_message, container, false);
                container.addView(view);
                initSignMessageTab(view);
            }
            else if (position == 1) {
                view = getLayoutInflater().inflate(R.layout.layout_verify_message, container, false);
                container.addView(view);
                initVerifyMessageTab(view);
            }
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

}
