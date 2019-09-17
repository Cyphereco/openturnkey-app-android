package com.cyphereco.openturnkey.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBTransItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;

import java.text.SimpleDateFormat;

public class TransDetailActivity extends AppCompatActivity {
    private final static String TAG = TransDetailActivity.class.getSimpleName();
    private OpenturnkeyDB mOtkDB = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trans_detail);

        ImageButton cancelBtn = findViewById(R.id.exit_btn_trans_detail);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Intent intent = this.getIntent();
        long transId = intent.getLongExtra("TRANS_ID", 0);

        mOtkDB = new OpenturnkeyDB(getApplicationContext());
        DBTransItem item = mOtkDB.getTransactionItemById(transId);
        if (null == item) {
            Log.d(TAG, "Cannot find transId: " + transId);
        }
        else {
            Log.d(TAG, "Find transId: " + transId);
            TextView tvStatusValue = findViewById(R.id.textView_trans_status_value);
            TextView tvDatetimeValue = findViewById(R.id.textView_trans_datetime_value);
            TextView tvPayerAddrValue = findViewById(R.id.textView_trans_payer_address_value);
            TextView tvPayeeAddrValue = findViewById(R.id.textView_trans_payee_address_value);
            TextView tvAmountValue = findViewById(R.id.textView_trans_amount_value);
            TextView tvFeeValue = findViewById(R.id.textView_trans_fee_value);
            TextView tvCommentValue = findViewById(R.id.textView_trans_comment_value);
            TextView tvRawdataValue = findViewById(R.id.textView_trans_rawData_value);

            tvStatusValue.setText(String.valueOf(item.getStatus()));
            tvDatetimeValue.setText(
                    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(item.getDatetime()));
            tvPayerAddrValue.setText(item.getPayerAddr());
            tvPayeeAddrValue.setText(item.getPayeeAddr());
            tvAmountValue.setText(String.valueOf(item.getAmount()));
            tvFeeValue.setText(String.valueOf(item.getFee()));
            tvCommentValue.setText(item.getComment());
            tvRawdataValue.setText(item.getRawData());
        }
    }

}
