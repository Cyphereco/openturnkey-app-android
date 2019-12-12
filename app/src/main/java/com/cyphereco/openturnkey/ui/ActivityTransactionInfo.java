package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.DBTransItem;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ActivityTransactionInfo extends AppCompatActivity {
    private final static String TAG = ActivityTransactionInfo.class.getSimpleName();

    private ImageView mBtnResend;
    private ImageView mBtnDelete;
    private Button mBtnExportRawData;
    private Button mBtnViewOnline;
    private ImageButton mBtnPreviousItem;
    private ImageButton mBtnNextItem;

    private OpenturnkeyDB mOtkDB = null;
    private List<DBTransItem> mTransactionDataSet;
    private int mCurrentPosition = 0;

    public static final String KEY_CURRENT_TRANS_ID = "CURRENT_TRANS_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_info);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Toolbar toolbar = findViewById(R.id.toolbar_tx_info_info);
        setSupportActionBar(toolbar);
        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mBtnResend = findViewById(R.id.iv_icon_tx_info_resend);
        mBtnDelete = findViewById(R.id.iv_icon_tx_info_delete);
        mBtnExportRawData = findViewById(R.id.btn_tx_info_export_raw_data);
        mBtnViewOnline = findViewById(R.id.btn_tx_info_view_online);
        mBtnPreviousItem = findViewById(R.id.btn_tx_info_previous_item);
        mBtnNextItem = findViewById(R.id.btn_tx_info_next_item);

        configUI();

        Intent intent = this.getIntent();
        long selectedTransId = intent.getLongExtra(KEY_CURRENT_TRANS_ID, 0);

        mOtkDB = new OpenturnkeyDB(getApplicationContext());
        getDatasetFromDB();

        DBTransItem item = null;
        for (int idx = 0; idx < mTransactionDataSet.size(); idx++) {
            if (selectedTransId == mTransactionDataSet.get(idx).getId()) {
                item = mTransactionDataSet.get(idx);
                mCurrentPosition = idx;
                break;
            }
        }

        if (null == item) {
            Toast.makeText(this, getString(R.string.transaction_not_found),
                    Toast.LENGTH_LONG).show();
            finishActivity();
        }
        else {
            // Show transaction information
            showTransactionInfo(item);
        }
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

    private void configUI() {
        mBtnResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processResendBtnClick();
            }
        });
        mBtnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processDeleteBtnClick();
            }
        });
        mBtnExportRawData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRawDataDialog();
            }
        });
        mBtnViewOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processViewOnLineBtnClick();
            }
        });
        mBtnPreviousItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mCurrentPosition - 1) >= 0) {
                    mCurrentPosition -= 1;
                    showTransactionInfo(mTransactionDataSet.get(mCurrentPosition));
                }
            }
        });
        mBtnNextItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mCurrentPosition + 1) < mTransactionDataSet.size()) {
                    mCurrentPosition += 1;
                    showTransactionInfo(mTransactionDataSet.get(mCurrentPosition));
                }
            }
        });
    }

    private void processResendBtnClick() {
        Log.d(TAG, "processResendBtnClick");
        DBTransItem item = mTransactionDataSet.get(mCurrentPosition);
        Intent intent = new Intent();
        intent.putExtra("REPAY_ADDRESS", item.getPayeeAddr());
        intent.putExtra("REPAY_AMOUNT", String.format(Locale.getDefault(), "%.8f", item.getAmount()));
        setResult(MainActivity.REQUEST_RESULT_CODE_REPAY, intent);
        finish();
    }

    private void processDeleteBtnClick() {
        Log.d(TAG, "processDeleteBtnClick");
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.delete_this_transaction_record))
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setPositiveButton(R.string.delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DBTransItem oriItem = mTransactionDataSet.get(mCurrentPosition);
                                DBTransItem previousItem = null;
                                DBTransItem nextItem = null;

                                if ((mCurrentPosition - 1) >= 0) {
                                    previousItem = mTransactionDataSet.get(mCurrentPosition - 1);
                                }
                                if ((mCurrentPosition + 1) < mTransactionDataSet.size()) {
                                    nextItem = mTransactionDataSet.get(mCurrentPosition + 1);
                                }

                                if (!mOtkDB.deleteTransactionById(oriItem.getId())) {
                                    Toast.makeText(getApplicationContext(),
                                            getString(R.string.failed_to_delete),
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // Get new data set
                                getDatasetFromDB();

                                if (null != previousItem) {
                                    for (int i = 0; i < mTransactionDataSet.size(); i++) {
                                        if (previousItem.getId() ==
                                                mTransactionDataSet.get(i).getId()) {
                                            mCurrentPosition = i;
                                        }
                                    }
                                }
                                else if (null != nextItem) {
                                    for (int i = 0; i < mTransactionDataSet.size(); i++) {
                                        if (nextItem.getId() ==
                                                mTransactionDataSet.get(i).getId()) {
                                            mCurrentPosition = i;
                                        }
                                    }
                                }
                                else {
                                    if (mTransactionDataSet.size() > 0) {
                                        mCurrentPosition = 0;
                                    }
                                    else {
                                        mCurrentPosition = -1;
                                        showEmptyTransactionDialog();
                                    }
                                }
                                if (mCurrentPosition >= 0) {
                                    showTransactionInfo(mTransactionDataSet.get(mCurrentPosition));
                                }
                            }
                        })
                .show();
    }

    private void processViewOnLineBtnClick() {
        Log.d(TAG, "processViewOnLineBtnClick");
        Intent browserIntent;
        String uriString;
        DBTransItem item = mTransactionDataSet.get(mCurrentPosition);

        if (item.getAmountUnitString().equalsIgnoreCase("BTC")) {
            uriString = "https://www.blockchain.com/btc/tx/" + item.getHash();
            browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
            startActivity(browserIntent);
        }
    }

    private void showEmptyTransactionDialog() {
        new AlertDialog.Builder(this)
                .setMessage("There is no transaction")
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishActivity();
                            }
                        })
                .show();
    }

    private void showFailReasonDialog(String reason) {
        new AlertDialog.Builder(this)
                .setTitle("FAIL Reason")
                .setMessage(reason)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .show();
    }

    private void showRawDataDialog() {
        final DBTransItem item = mTransactionDataSet.get(mCurrentPosition);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.raw_data))
                .setMessage(item.getRawData())
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setPositiveButton(R.string.copy,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ClipboardManager clipboard =
                                        (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("rawdata",
                                        item.getRawData());
                                clipboard.setPrimaryClip(clip);
                            }
                        })
                .show();
    }

    private void finishActivity() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private void showTransactionInfo(final DBTransItem item) {
        TextView tvDateTime = findViewById(R.id.tv_tx_info_datetime);
        TextView tvResult = findViewById(R.id.tv_tx_info_result);
        ImageView ivHelpIcon = findViewById(R.id.iv_icon_tx_info_help);
        ImageView ivResultIcon = findViewById(R.id.iv_icon_tx_info_result);
        TextView tvFrom = findViewById(R.id.tv_tx_info_from);
        TextView tvTo = findViewById(R.id.tv_tx_info_to);
        TextView tvAmount = findViewById(R.id.tv_tx_info_amount);
        TextView tvAmountUnit = findViewById(R.id.tv_tx_info_amount_unit);
        TextView tvFee = findViewById(R.id.tv_tx_info_fees);
        TextView tvFeeUnit = findViewById(R.id.tv_tx_info_fees_unit);
        TextView tvTransactionId = findViewById(R.id.tv_tx_info_id);

        if (0 == item.getStatus()) {
            // Transaction status is successful
            ivHelpIcon.setVisibility(View.INVISIBLE);
            ivResultIcon.setImageResource(R.drawable.ic_success_24dp);
            mBtnViewOnline.setVisibility(View.VISIBLE);
            // append confirmation number
            int confirmations = item.getConfirmations();
            String c;
            if (confirmations < 0) {
                c = "(?)";
            }
            else {
                c = "(" + confirmations + ")";
            }
            tvResult.setText(getString(R.string.success) + c);
            tvTransactionId.setText(item.getHash());
        }
        else {
            // Transaction status is fail
            ivHelpIcon.setVisibility(View.VISIBLE);
            ivResultIcon.setImageResource(R.drawable.ic_fail_24dp);
            mBtnViewOnline.setVisibility(View.INVISIBLE);
            tvResult.setText(R.string.fail);
            tvTransactionId.setText("N/A");
            ivHelpIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFailReasonDialog(item.getComment());
                }
            });
        }

        tvDateTime.setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(
                item.getDatetime()));
        tvFrom.setText(item.getPayerAddr());
        tvTo.setText(item.getPayeeAddr());
        tvAmount.setText(getAmountString(item.getAmount()));
        tvAmountUnit.setText(item.getAmountUnitString());
        tvFee.setText(getAmountString(item.getFee()));
        tvFeeUnit.setText(item.getFeeUnitString());

        // Update button
        updatePageChangeBtn();
    }

    private void updatePageChangeBtn() {
        if (mCurrentPosition < (mTransactionDataSet.size() - 1)) {
            mBtnNextItem.setEnabled(true);
            if (null != mBtnNextItem.getDrawable()) {
                mBtnNextItem.getDrawable().setColorFilter(null);
            }
        }
        else {
            mBtnNextItem.setEnabled(false);
            if (null != mBtnNextItem.getDrawable()) {
                mBtnNextItem.getDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
            }
        }
        if (0 == mCurrentPosition) {
            mBtnPreviousItem.setEnabled(false);
            if (null != mBtnPreviousItem.getDrawable()) {
                mBtnPreviousItem.getDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
            }
        }
        else {
            mBtnPreviousItem.setEnabled(true);
            if (null != mBtnPreviousItem.getDrawable()) {
                mBtnPreviousItem.getDrawable().setColorFilter(null);
            }
        }
    }

    private String getAmountString(double src) {
        String result = String.format(Locale.getDefault(),"%.8f", src);
        return (result.substring(0, result.length() - 4) + " " +
                result.substring(result.length() - 4));
    }

    private void getDatasetFromDB() {
        mTransactionDataSet = mOtkDB.getAllTransaction();

        Collections.sort(mTransactionDataSet, new Comparator<DBTransItem>() {
            @Override
            public int compare(DBTransItem o1, DBTransItem o2) {
                Date dt1 = new Date(o1.getDatetime());
                Date dt2 = new Date(o2.getDatetime());
                if (dt1.before(dt2)) {
                    return 1;
                }
                else if (dt1.equals(dt2)) {
                    return 0;
                }
                return -1;
            }
        });

    }
}
