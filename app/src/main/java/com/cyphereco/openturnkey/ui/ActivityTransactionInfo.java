package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.slf4j.Logger;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.RecordTransaction;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityTransactionInfo extends AppCompatActivity {
    private final static String TAG = ActivityTransactionInfo.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private ImageView ivTxDelete;
    private ImageView ivRawData;
    private ImageView ivViewOnline;
    private ImageButton btnPrevious;
    private ImageButton btnNext;

    private List<RecordTransaction> mTransactionDataSet;
    private int mCurrentPosition = 0;

    public static final String KEY_CURRENT_TRANS_ID = "CURRENT_TRANS_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_info);

        Toolbar toolbar = findViewById(R.id.toolbar_tx_info_info);
        setSupportActionBar(toolbar);
        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ivTxDelete = findViewById(R.id.iv_tx_delete);
        ivRawData = findViewById(R.id.iv_raw_data);
        ivViewOnline = findViewById(R.id.iv_view_online);

        btnPrevious = findViewById(R.id.btn_tx_info_previous_item);
        btnNext = findViewById(R.id.btn_tx_info_next_item);

        configUI();

        Intent intent = this.getIntent();
        long selectedTransId = intent.getLongExtra(KEY_CURRENT_TRANS_ID, 0);

        getDatasetFromDB();

        RecordTransaction item = null;
        for (int idx = 0; idx < mTransactionDataSet.size(); idx++) {
            if (selectedTransId == mTransactionDataSet.get(idx).getId()) {
                item = mTransactionDataSet.get(idx);
                mCurrentPosition = idx;
                break;
            }
        }

        if (null == item) {
            AlertPrompt.alert(this, getString(R.string.transaction_not_found));
            finishActivity();
        } else {
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
        return super.onOptionsItemSelected(item);
    }

    private void configUI() {
        ivTxDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processDeleteBtnClick();
            }
        });
        ivRawData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRawDataDialog();
            }
        });
        ivViewOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processViewOnLineBtnClick();
            }
        });
        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mCurrentPosition - 1) >= 0) {
                    mCurrentPosition -= 1;
                    showTransactionInfo(mTransactionDataSet.get(mCurrentPosition));
                }
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mCurrentPosition + 1) < mTransactionDataSet.size()) {
                    mCurrentPosition += 1;
                    showTransactionInfo(mTransactionDataSet.get(mCurrentPosition));
                }
            }
        });
    }

    private void processDeleteBtnClick() {
        logger.debug("processDeleteBtnClick");
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
                                RecordTransaction oriItem = mTransactionDataSet.get(mCurrentPosition);
                                RecordTransaction previousItem = null;
                                RecordTransaction nextItem = null;

                                if ((mCurrentPosition - 1) >= 0) {
                                    previousItem = mTransactionDataSet.get(mCurrentPosition - 1);
                                }
                                if ((mCurrentPosition + 1) < mTransactionDataSet.size()) {
                                    nextItem = mTransactionDataSet.get(mCurrentPosition + 1);
                                }

                                if (!OpenturnkeyDB.deleteTransactionById(oriItem.getId())) {
                                    AlertPrompt.alert(getApplicationContext(), getString(R.string.failed_to_delete));
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
                                } else if (null != nextItem) {
                                    for (int i = 0; i < mTransactionDataSet.size(); i++) {
                                        if (nextItem.getId() ==
                                                mTransactionDataSet.get(i).getId()) {
                                            mCurrentPosition = i;
                                        }
                                    }
                                } else {
                                    if (mTransactionDataSet.size() > 0) {
                                        mCurrentPosition = 0;
                                    } else {
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
        logger.debug("processViewOnLineBtnClick");
        Intent browserIntent;
        String uriString;
        RecordTransaction item = mTransactionDataSet.get(mCurrentPosition);

        uriString = "https://www.blockchain.com/btc/tx/" + item.getHash();
        browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        startActivity(browserIntent);
    }

    private void showEmptyTransactionDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.no_payment_records)
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
                .setTitle(R.string.fail_reason)
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
        final RecordTransaction item = mTransactionDataSet.get(mCurrentPosition);

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
                                if (clipboard != null) {
                                    clipboard.setPrimaryClip(clip);
                                }
                            }
                        })
                .show();
    }

    private void finishActivity() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private void showTransactionInfo(final RecordTransaction item) {
        TextView tvDateTime = findViewById(R.id.tv_tx_info_datetime);
        TextView tvResult = findViewById(R.id.tv_tx_info_result);
        TextView tvFrom = findViewById(R.id.tv_tx_info_from);
        TextView tvTo = findViewById(R.id.tv_tx_info_to);
        TextView tvAmount = findViewById(R.id.tv_tx_info_amount);
        TextView tvAmountUnit = findViewById(R.id.tv_tx_info_amount_unit);
        TextView tvFee = findViewById(R.id.tv_tx_info_fees);
        TextView tvFeeUnit = findViewById(R.id.tv_tx_info_fees_unit);
        TextView tvTransactionId = findViewById(R.id.tv_tx_info_id);

//        if (0 == item.getStatus()) {
//            // Transaction status is successful
//            btnViewOnLine.setVisibility(View.VISIBLE);
//            // append confirmation number
//            int confirmations = item.getConfirmations();
//            String c;
//            if (confirmations < 1) {
//                c = "0 " + getString(R.string.confirmation);
//            } else {
//                ivHelpIcon.setVisibility(View.INVISIBLE);
//                ivResultIcon.setImageResource(R.drawable.ic_success_24dp);
//                c = confirmations + " " + getString(R.string.confirmation);
//
////                if (confirmations > 6){
////                    c = "> 144 " + getString(R.string.confirmation);
////                }
//            }
//
//            tvResult.setText(c);
//            tvTransactionId.setText(item.getHash());
//        } else {
//            // Transaction status is fail
//            ivHelpIcon.setVisibility(View.VISIBLE);
//            ivResultIcon.setImageResource(R.drawable.ic_fail_24dp);
//            btnViewOnLine.setVisibility(View.INVISIBLE);
//            tvResult.setText(R.string.fail);
//            tvTransactionId.setText("N/A");
//            ivHelpIcon.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    showFailReasonDialog(item.getComment());
//                }
//            });
//        }

//        tvDateTime.setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(
//                item.getTimestamp()));
//        tvFrom.setText(item.getPayer());
//        tvTo.setText(item.getPayee());
//        tvAmount.setText(formattedAmount(item.getAmount()));
//        tvAmountUnit.setText(item.getAmountUnitString());
//        tvFee.setText(formattedAmount(item.getFee()));
//        tvFeeUnit.setText(item.getFeeUnitString());

        // Update button
        updatePageChangeBtn();
    }

    private void updatePageChangeBtn() {
        if (mCurrentPosition < (mTransactionDataSet.size() - 1)) {
            btnNext.setEnabled(true);
            if (null != btnNext.getDrawable()) {
                btnNext.getDrawable().setColorFilter(null);
            }
        } else {
            btnNext.setEnabled(false);
            if (null != btnNext.getDrawable()) {
                btnNext.getDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
            }
        }
        if (0 == mCurrentPosition) {
            btnPrevious.setEnabled(false);
            if (null != btnPrevious.getDrawable()) {
                btnPrevious.getDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
            }
        } else {
            btnPrevious.setEnabled(true);
            if (null != btnPrevious.getDrawable()) {
                btnPrevious.getDrawable().setColorFilter(null);
            }
        }
    }

    private String formattedAmount(double src) {
        String result = String.format(Locale.getDefault(), "%.8f", src);
        return (result.substring(0, result.length() - 4) + " " +
                result.substring(result.length() - 4));
    }

    private void getDatasetFromDB() {
        mTransactionDataSet = OpenturnkeyDB.getAllTransactions();

        Collections.sort(mTransactionDataSet, new Comparator<RecordTransaction>() {
            @Override
            public int compare(RecordTransaction o1, RecordTransaction o2) {
                Date dt1 = new Date(o1.getTimestamp());
                Date dt2 = new Date(o2.getTimestamp());
                if (dt1.before(dt2)) {
                    return 1;
                } else if (dt1.equals(dt2)) {
                    return 0;
                }
                return -1;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.setCurrentActivity(getClass().getName());
    }
}
