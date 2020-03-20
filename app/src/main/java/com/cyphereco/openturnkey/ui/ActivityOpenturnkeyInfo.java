package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.QRCodeUtils;
import com.cyphereco.openturnkey.webservices.BlockChainInfo;
import com.sandro.bitcoinpaymenturi.BitcoinPaymentURI;

import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

public class ActivityOpenturnkeyInfo extends AppCompatActivity {
    public static final String TAG = ActivityOpenturnkeyInfo.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private static OtkData otkData;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        otkData = (OtkData) getIntent().getSerializableExtra(MainActivity.KEY_OTK_DATA);

        // inflate layout
        setContentView(R.layout.activity_openturnkey_info);

        // configure toolbar and home buttom
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.setCurrentActivity(getClass().getName());

        if (otkData == null) {
            MainActivity.setCurrentActivity(MainActivity.class.getName());
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            // show local currency label
            final LocalCurrency localCurrency = Preferences.getLocalCurrency();
            TextView labelFiat = findViewById(R.id.label_fiat);
            labelFiat.setText(localCurrency.toString());

            // update lock state
            ImageView ivLockIcon = findViewById(R.id.lock_state);
            if (otkData.getOtkState().getLockState() != OtkState.LockState.UNLOCKED) {
                ivLockIcon.setImageResource(R.drawable.ic_lock_outline_black_24dp);
                ivLockIcon.setColorFilter(Color.argb(255, 0xf4, 0x43, 0x36));
            } else {
                ivLockIcon.setImageResource(R.drawable.ic_lock_open_black_24dp);
                ivLockIcon.setColorFilter(Color.argb(0xff, 0x4c, 0xaf, 0x50));
            }

            // show power level
            int batLevel = otkData.getMintInfo().getBatteryLevel();
            TextView tvPowerLevel = findViewById(R.id.power_state_value);
            tvPowerLevel.setText(String.format(Locale.ENGLISH, "%d%%", batLevel));

            // show power level icon
            ImageView ivPowerIcon = findViewById(R.id.power_state);
            if (batLevel < 20) {
                ivPowerIcon.setImageResource(R.drawable.ic_battery_20_black_24dp);
            } else if (batLevel < 50) {
                ivPowerIcon.setImageResource(R.drawable.ic_battery_50_black_24dp);
            } else if (batLevel < 80) {
                ivPowerIcon.setImageResource(R.drawable.ic_battery_80_black_24dp);
            } else {
                ivPowerIcon.setImageResource(R.drawable.ic_battery_full_black_24dp);
            }

            // show btc address
            final String address = otkData.getSessionData().getAddress();
            final TextView tvAddress = findViewById(R.id.btc_address_context);
            tvAddress.setText(address);
            tvAddress.setMovementMethod(LinkMovementMethod.getInstance());
            tvAddress.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String url = "https://bitref.com/" + tvAddress.getText().toString();
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            });

            // construct QR code
            BitcoinPaymentURI uri = new BitcoinPaymentURI.Builder().address(address).build();
            if (uri != null) {
                ImageView ivQrCode = findViewById(R.id.btc_address_qrcode);
                Bitmap bitmap = QRCodeUtils.encodeAsBitmap(uri.getURI(), ivQrCode.getDrawable().getIntrinsicWidth(), ivQrCode.getDrawable().getIntrinsicHeight());
                ivQrCode.setImageBitmap(bitmap);
            }

            // show note
            TextView tvNote = findViewById(R.id.user_note_context);
            tvNote.setText(otkData.getMintInfo().getNote());

            // configure copy address button
            ImageView ivCopy = findViewById(R.id.icon_copy);
            ivCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("address", address);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                    }
                    AlertPrompt.info(getApplicationContext(), getString(R.string.address_copied));
                }
            });

            // configure mint information button
            ImageView ivMintInfo = findViewById(R.id.icon_mint_information);
            ivMintInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Show dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityOpenturnkeyInfo.this);
                    builder.setTitle(R.string.mint_information)
                            .setMessage(otkData.getMintInfo().toString())
                            .setPositiveButton(R.string.ok, null)
                            .setCancelable(false)
                            .show();
                }
            });

            // show balance
            final TextView tvBalanceBtc = findViewById(R.id.text_balance_btc);
            tvBalanceBtc.setText(R.string.fetching);
            final TextView tvBalanceFiat = findViewById(R.id.text_balance_fiat);
            tvBalanceFiat.setText(R.string.fetching);

            // handler for updating balance
            handler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    BigDecimal balance = (BigDecimal) msg.obj;
                    if (balance.equals(BigDecimal.valueOf(-1))) {
                        tvBalanceBtc.setText(R.string.cannot_reach_network);
                        tvBalanceFiat.setText(R.string.cannot_reach_network);
                        return false;
                    }
                    double btc = BtcUtils.satoshiToBtc(balance.longValue());
                    tvBalanceBtc.setText(String.format(Locale.ENGLISH, "%.8f", btc));
                    double lcAmount = BtcUtils.btcToLocalCurrency(MainActivity.getBtcExchangeRates(), localCurrency, btc);
                    tvBalanceFiat.setText(String.format(Locale.ENGLISH, "%.2f", lcAmount));
                    return false;
                }
            });

            BlockChainInfo.getBalance(address, new BlockChainInfo.WebResultHandler() {
                @Override
                public void onBalanceUpdated(BigDecimal balance) {
                    Message msg = new Message();
                    msg.obj = balance;
                    handler.sendMessage(msg);
                }

                @Override
                public void onBlockHeightUpdated(int height) {

                }

                @Override
                public void onTxBlockHeightUpdated(int height) {

                }

                @Override
                public void onRawTxUpdated(String rawTx) {

                }

                @Override
                public void onConfirmationsUpdated(int confirmations) {

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        otkData = null;
    }
}
