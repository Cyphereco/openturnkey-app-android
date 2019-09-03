package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Otk;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.OtkEvent;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.QRCodeUtils;
import com.sandro.bitcoinpaymenturi.BitcoinPaymentURI;

import java.math.BigDecimal;

public class OpenturnkeyInfoActivity extends AppCompatActivity {
    public static final String TAG = OpenturnkeyInfoActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_openturnkey_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        final OtkData otkData = (OtkData)intent.getSerializableExtra(MainActivity.KEY_OTK_DATA);
        if (otkData == null) {
            return;
        }

        final LocalCurrency lc = Preferences.getLocalCurrency(getApplicationContext());
        TextView tv = findViewById(R.id.label_fiat);

        tv.setText(lc.toString());

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "handle message:" + msg.what);
                TextView tv = findViewById(R.id.text_balance_btc);
                OtkEvent event = (OtkEvent) msg.obj;
                BigDecimal b = event.getBalance();
                double btc = BtcUtils.satoshiToBtc(b.longValue());
                if (b != null) {
                    tv.setText(String.format("%.8f", btc));
                    tv = findViewById(R.id.text_balance_fiat);
                    double lcAmount = BtcUtils.btcToLocalCurrency(event.getCurrencyExRate(), lc, btc);
                    tv.setText(String.format("%.2f", lcAmount));
                }
                else {
                    tv.setText(R.string._numerical_zero);
                    tv = findViewById(R.id.text_balance_fiat);
                    tv.setText(R.string._numerical_zero);
                }


            }
        };
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
        if (OtkState.LockState.LOCKED == otkData.getOtkState().getLockState()) {
            iv.setImageResource(R.drawable.ic_lock_outline_black_24dp);
        }
        else {
            iv.setImageResource(R.drawable.ic_lock_open_black_24dp);
        }

        // Battery level
        int batLevel = otkData.getMintInfo().getBatteryLevel();
        iv = findViewById(R.id.power_state);
        if (batLevel < 20 ) {
            iv.setImageResource(R.drawable.ic_battery_20_black_24dp);
        }
        else if (batLevel < 50) {
            iv.setImageResource(R.drawable.ic_battery_50_black_24dp);
        }
        else if (batLevel < 80) {
            iv.setImageResource(R.drawable.ic_battery_80_black_24dp);
        }
        else {
            iv.setImageResource(R.drawable.ic_battery_full_black_24dp);
        }

        tv = findViewById(R.id.power_state_value);
        tv.setText(String.format("%d%%", batLevel));

        // Note
        tv = findViewById(R.id.user_note_context);
        tv.setText(otkData.getMintInfo().getNote());

        // Balance
        tv = findViewById(R.id.text_balance_btc);
        tv.setText(R.string.fetching);
        tv = findViewById(R.id.text_balance_fiat);
        tv.setText(R.string.fetching);
        Otk otk = Otk.getInstance();
        otk.setBalanceListener(new Otk.BalanceUpdateListener() {
            @Override
            public void onOtkEvent(OtkEvent event) {
                if (event.getType() == OtkEvent.Type.BALANCE_UPDATE) {
                    if (!event.getAddress().equals(address)) {
                        Log.d(TAG, "Address doesn't match." + event.getAddress() + " " + address);
                        return;
                    }
                    Message msg = new Message();
                    msg.obj = event;
                    handler.sendMessage(msg);
                }
            }
        });
        otk.getBalance(address);

        // i button
        ImageView i = findViewById(R.id.icon_mint_information);
        i.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(OpenturnkeyInfoActivity.this);
                AlertDialog d = builder.setTitle(R.string.mint_inforamtion)
                        .setMessage(otkData.getMintInfo().toString())
                        .setPositiveButton(R.string.ok, null)
                        .setCancelable(false)
                        .show();
            }
        });

        // i button
        ImageView copy = findViewById(R.id.icon_copy);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("address", address);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), R.string.address_copied, Toast.LENGTH_SHORT).show();
            }
        });



    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Otk.getInstance().setBalanceListener(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
