package com.cyphereco.openturnkey.ui;

import android.annotation.SuppressLint;
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
import com.cyphereco.openturnkey.core.Otk;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.OtkEvent;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.utils.QRCodeUtils;
import com.sandro.bitcoinpaymenturi.BitcoinPaymentURI;

import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

public class ActivityOpenturnkeyInfo extends AppCompatActivity {
    public static final String TAG = ActivityOpenturnkeyInfo.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    static private Otk mOtk = null;
    static Handler handler = null;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug("onCreate");
        setContentView(R.layout.activity_openturnkey_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        final OtkData otkData = (OtkData) intent.getSerializableExtra(MainActivity.KEY_OTK_DATA);
        if (otkData == null) {
            return;
        }

        mOtk = Otk.getInstance(getApplicationContext());

        final LocalCurrency lc = Preferences.getLocalCurrency();
        TextView tv = findViewById(R.id.label_fiat);

        tv.setText(lc.toString());

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                logger.debug("handle message:" + msg.what);
                TextView tv = findViewById(R.id.text_balance_btc);
                OtkEvent event = (OtkEvent) msg.obj;
                BigDecimal b = event.getBalance();
                if (b.equals(BigDecimal.valueOf(-1))) {
                    tv.setText(R.string.cannot_reach_network);
                    tv = findViewById(R.id.text_balance_fiat);
                    tv.setText(R.string.cannot_reach_network);
                    return;
                }
                double btc = BtcUtils.satoshiToBtc(b.longValue());
                tv.setText(String.format(Locale.ENGLISH, "%.8f", btc));
                tv = findViewById(R.id.text_balance_fiat);
                double lcAmount = BtcUtils.btcToLocalCurrency(event.getCurrencyExRate(), lc, btc);
                tv.setText(String.format(Locale.ENGLISH, "%.2f", lcAmount));
            }
        };

        final TextView txt = findViewById(R.id.btc_address_context);
        txt.setMovementMethod(LinkMovementMethod.getInstance());
        txt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String url = "https://bitref.com/" + txt.getText().toString();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

        updateInfo(otkData);
    }

    private void updateInfo(final OtkData otkData) {
        final LocalCurrency lc = Preferences.getLocalCurrency();
        TextView tv = findViewById(R.id.label_fiat);

        tv.setText(lc.toString());
        // Check balance
        tv = findViewById(R.id.btc_address_context);
        final String address = otkData.getSessionData().getAddress();
        tv.setText(address);
        BitcoinPaymentURI uri = new BitcoinPaymentURI.Builder().address(address).build();
        if (uri != null) {
            ImageView iv = findViewById(R.id.btc_address_qrcode);
            Bitmap bitmap = QRCodeUtils.encodeAsBitmap(uri.getURI(), iv.getDrawable().getIntrinsicWidth(), iv.getDrawable().getIntrinsicHeight());
            iv.setImageBitmap(bitmap);
        }

        // Lock state
        ImageView iv = findViewById(R.id.lock_state);
        if (OtkState.LockState.UNLOCKED != otkData.getOtkState().getLockState()) {
            iv.setImageResource(R.drawable.ic_lock_outline_black_24dp);
            iv.setColorFilter(Color.argb(255, 0xf4, 0x43, 0x36));
        } else {
            iv.setImageResource(R.drawable.ic_lock_open_black_24dp);
            iv.setColorFilter(Color.argb(0xff, 0x4c, 0xaf, 0x50));
        }

        // Battery level
        int batLevel = otkData.getMintInfo().getBatteryLevel();
        iv = findViewById(R.id.power_state);
        if (batLevel < 20) {
            iv.setImageResource(R.drawable.ic_battery_20_black_24dp);
        } else if (batLevel < 50) {
            iv.setImageResource(R.drawable.ic_battery_50_black_24dp);
        } else if (batLevel < 80) {
            iv.setImageResource(R.drawable.ic_battery_80_black_24dp);
        } else {
            iv.setImageResource(R.drawable.ic_battery_full_black_24dp);
        }

        tv = findViewById(R.id.power_state_value);
        tv.setText(String.format(Locale.ENGLISH, "%d%%", batLevel));

        // Note
        tv = findViewById(R.id.user_note_context);
        tv.setText(otkData.getMintInfo().getNote());

        // Balance
        tv = findViewById(R.id.text_balance_btc);
        tv.setText(R.string.fetching);
        tv = findViewById(R.id.text_balance_fiat);
        tv.setText(R.string.fetching);

        mOtk.setBalanceListener(new Otk.BalanceUpdateListener() {
            @Override
            public void onOtkEvent(OtkEvent event) {
                if (event.getType() == OtkEvent.Type.BALANCE_UPDATE) {
                    if (!event.getAddress().equals(address)) {
                        logger.debug("Address doesn't match." + event.getAddress() + " " + address);
                        return;
                    }
                    Message msg = new Message();
                    msg.obj = event;
                    handler.sendMessage(msg);
                }
            }
        });
        mOtk.getBalance(address);

        // i button
        ImageView i = findViewById(R.id.icon_mint_information);
        i.setOnClickListener(new View.OnClickListener() {
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

        // copy button
        ImageView copy = findViewById(R.id.icon_copy);
        copy.setOnClickListener(new View.OnClickListener() {
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logger.debug("onDestroy");
        if (mOtk == null) {
            logger.debug("mOtk is null");
            return;
        }
        mOtk.setBalanceListener(null);
        mOtk = null;
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
    }
}
