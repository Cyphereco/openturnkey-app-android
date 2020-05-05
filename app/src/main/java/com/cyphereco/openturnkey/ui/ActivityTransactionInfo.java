package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.db.RecordTransaction;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcExchangeRates;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ActivityTransactionInfo extends AppCompatActivity {
    private final static String TAG = ActivityTransactionInfo.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    private ImageView ivTxDelete;
    private ImageView ivRawData;
    private ImageView ivViewOnline;
    private ImageView ivExchangeRate;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private Switch switchShowLocalCurrency;

    private List<RecordTransaction> listRecordTransactions;
    private int idxCurrentPosition = 0;

    public static final String KEY_CURRENT_TRANS_ID = "CURRENT_TRANS_ID";

    private float x1;
    static final int MIN_DISTANCE = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_info);

        Toolbar toolbar = findViewById(R.id.toolbar_tx_info_title);
        setSupportActionBar(toolbar);
        if (null != getSupportActionBar()) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ivTxDelete = findViewById(R.id.iv_tx_info_delete);
        ivRawData = findViewById(R.id.iv_tx_info_raw_data);
        ivViewOnline = findViewById(R.id.iv_tx_info_view_online);
        ivExchangeRate = findViewById(R.id.iv_tx_info_exchange_rate);

        btnPrevious = findViewById(R.id.btn_tx_info_previous_item);
        btnNext = findViewById(R.id.btn_tx_info_next_item);
        switchShowLocalCurrency = findViewById(R.id.switch_tx_info_show_local_currency);

        setViewOnClickListener();

        Intent intent = this.getIntent();
        long selectedTransId = intent.getLongExtra(KEY_CURRENT_TRANS_ID, 0);

        getDatasetFromDB();

        RecordTransaction recordTransaction = null;
        for (int idx = 0; idx < listRecordTransactions.size(); idx++) {
            if (selectedTransId == listRecordTransactions.get(idx).getId()) {
                recordTransaction = listRecordTransactions.get(idx);
                idxCurrentPosition = idx;
                break;
            }
        }

        if (null == recordTransaction) {
            AlertPrompt.alert(this, getString(R.string.transaction_not_found));
            finishActivity();
        } else {
            // Show transaction information
            showTransactionInfo(recordTransaction);
        }

        switchShowLocalCurrency.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showCurrentTransaction();
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

    private void setViewOnClickListener() {
        ivTxDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnDeleteClick();
            }
        });
        ivRawData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRawDataDialog();
            }
        });
        ivExchangeRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExchangeRate();
            }
        });
        ivViewOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnViewOnlineClick();
            }
        });
        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((idxCurrentPosition - 1) >= 0) {
                    idxCurrentPosition -= 1;
                    showTransactionInfo(listRecordTransactions.get(idxCurrentPosition));
                }
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((idxCurrentPosition + 1) < listRecordTransactions.size()) {
                    idxCurrentPosition += 1;
                    showTransactionInfo(listRecordTransactions.get(idxCurrentPosition));
                }
            }
        });
    }

    private void onBtnDeleteClick() {
        logger.debug("processDeleteBtnClick");
        Dialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogNarrowWidth)
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
                                RecordTransaction oriItem = listRecordTransactions.get(idxCurrentPosition);
                                RecordTransaction previousItem = null;
                                RecordTransaction nextItem = null;

                                if ((idxCurrentPosition - 1) >= 0) {
                                    previousItem = listRecordTransactions.get(idxCurrentPosition - 1);
                                }
                                if ((idxCurrentPosition + 1) < listRecordTransactions.size()) {
                                    nextItem = listRecordTransactions.get(idxCurrentPosition + 1);
                                }

                                if (!OpenturnkeyDB.deleteTransactionById(oriItem.getId())) {
                                    AlertPrompt.alert(getApplicationContext(), getString(R.string.failed_to_delete));
                                    return;
                                }

                                // Get new data set
                                getDatasetFromDB();

                                if (null != previousItem) {
                                    for (int i = 0; i < listRecordTransactions.size(); i++) {
                                        if (previousItem.getId() ==
                                                listRecordTransactions.get(i).getId()) {
                                            idxCurrentPosition = i;
                                        }
                                    }
                                } else if (null != nextItem) {
                                    for (int i = 0; i < listRecordTransactions.size(); i++) {
                                        if (nextItem.getId() ==
                                                listRecordTransactions.get(i).getId()) {
                                            idxCurrentPosition = i;
                                        }
                                    }
                                } else {
                                    if (listRecordTransactions.size() > 0) {
                                        idxCurrentPosition = 0;
                                    } else {
                                        idxCurrentPosition = -1;
                                        showEmptyTransactionDialog();
                                    }
                                }
                                if (idxCurrentPosition >= 0) {
                                    showTransactionInfo(listRecordTransactions.get(idxCurrentPosition));
                                }
                            }
                        })
                .create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;

        dialog.show();
    }

    private void onBtnViewOnlineClick() {
        logger.debug("processViewOnLineBtnClick");
        Intent browserIntent;
        String uriString;
        RecordTransaction item = listRecordTransactions.get(idxCurrentPosition);

        uriString = "https://www.blockchain.com/btc/tx/" + item.getHash();
        if (Preferences.isTestnet()) {
            uriString = "https://live.blockcypher.com/btc-testnet/tx/" + item.getHash();
        }
        browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        startActivity(browserIntent);
    }

    private void showEmptyTransactionDialog() {
        Dialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogNarrowWidth)
                .setMessage(R.string.no_payment_records)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishActivity();
                            }
                        })
                .create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;

        dialog.show();
    }

    private void showRawDataDialog() {
        final RecordTransaction item = listRecordTransactions.get(idxCurrentPosition);

        Dialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogNarrowWidth)
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
                .create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;

        dialog.show();
    }

    private void showExchangeRate() {
        RecordTransaction recordTransaction = listRecordTransactions.get(idxCurrentPosition);
        BtcExchangeRates loggedExchangeRate = new BtcExchangeRates(recordTransaction.getExchangeRate());

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_exchange_rates, null);

        ((TextView) view.findViewById(R.id.tv_dialog_exchange_rate_datetime))
                .setText(((TextView) findViewById(R.id.tv_tx_info_datetime)).getText());
        ((TextView) view.findViewById(R.id.tv_dialog_exhange_rate_amount_cny))
                .setText(formattedFiatAmount(loggedExchangeRate.getRate_cny()));
        ((TextView) view.findViewById(R.id.tv_dialog_exhange_rate_amount_eur))
                .setText(formattedFiatAmount(loggedExchangeRate.getRate_eur()));
        ((TextView) view.findViewById(R.id.tv_dialog_exhange_rate_amount_jpy))
                .setText(formattedFiatAmount(loggedExchangeRate.getRate_jpy()));
        ((TextView) view.findViewById(R.id.tv_dialog_exhange_rate_amount_twd))
                .setText(formattedFiatAmount(loggedExchangeRate.getRate_twd()));
        ((TextView) view.findViewById(R.id.tv_dialog_exhange_rate_amount_usd))
                .setText(formattedFiatAmount(loggedExchangeRate.getRate_usd()));

        Dialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogNarrowWidth)
                .setTitle(R.string.exchange_rate)
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;

        dialog.show();
    }

    private void finishActivity() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private void showCurrentTransaction() {
        showTransactionInfo(listRecordTransactions.get(idxCurrentPosition));
    }

    private void showTransactionInfo(final RecordTransaction recordTransaction) {
        logger.debug("Dump recordTransaction: {}", recordTransaction.toString());
//        logger.debug("Tx block height: {}", recordTransaction.getBlockHeight());
        BtcExchangeRates loggedExchangeRate = new BtcExchangeRates(recordTransaction.getExchangeRate());
//        logger.debug("loggedExchangeRate: {}", loggedExchangeRate);

        TextView tvDateTime = findViewById(R.id.tv_tx_info_datetime);
        TextView tvResult = findViewById(R.id.tv_tx_info_result);
        TextView tvSender = findViewById(R.id.tv_tx_info_sender);
        TextView tvSendAmount = findViewById(R.id.tv_tx_info_send_amount);
        TextView tvSendAmountUnit = findViewById(R.id.label_tx_info_send_amount_unit);
        TextView tvRecipient = findViewById(R.id.tv_tx_info_recipient);
        TextView tvRecvAmount = findViewById(R.id.tv_tx_info_recv_amount);
        TextView tvRecvAmountUnit = findViewById(R.id.label_tx_info_recv_amount_unit);
        TextView tvFees = findViewById(R.id.tv_tx_info_fees);
        TextView tvFeesUnit = findViewById(R.id.label_tx_info_fees_unit);
        TextView tvHash = findViewById(R.id.tv_tx_info_hash);

        tvSendAmountUnit.setText(getString(R.string._unit_btc));
        tvRecvAmountUnit.setText(getString(R.string._unit_btc));
        tvFeesUnit.setText(getString(R.string._unit_btc));

        long confirmations = MainActivity.getBlockHeight() - recordTransaction.getBlockHeight() + 1;
        if (recordTransaction.getBlockHeight() <= 0 || confirmations < 0) {
            tvResult.setText(R.string.unconfirmed);
        } else if (confirmations > 1440) {
            tvResult.setText("1440+ " + getString(R.string.confirmation));
        }
        else {
            tvResult.setText(String.format(Locale.getDefault(), "%d %s", confirmations, getString(R.string.confirmation)));
        }

        double amountSent = recordTransaction.getAmountSent();
        double amountRecv = recordTransaction.getAmountRecv();
        double fees = amountSent - amountRecv;

        if (switchShowLocalCurrency.isChecked()) {
            String strUnit = Preferences.getLocalCurrency().toString();
            amountSent = BtcUtils.btcToLocalCurrency(loggedExchangeRate, Preferences.getLocalCurrency(), amountSent);
            amountRecv = BtcUtils.btcToLocalCurrency(loggedExchangeRate, Preferences.getLocalCurrency(), amountRecv);
            fees = amountSent - amountRecv;

            tvSendAmountUnit.setText(strUnit);
            tvRecvAmountUnit.setText(strUnit);
            tvFeesUnit.setText(strUnit);
        }

        ivViewOnline.setVisibility(View.VISIBLE);

        tvDateTime.setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(
                recordTransaction.getTimestamp()));
        tvSender.setText(recordTransaction.getPayer());
        tvSendAmount.setText(switchShowLocalCurrency.isChecked() ?
                formattedFiatAmount(amountSent) : formattedBtcAmount(amountSent));

        tvRecipient.setText(recordTransaction.getPayee());
        tvRecvAmount.setText(switchShowLocalCurrency.isChecked() ?
                formattedFiatAmount(amountRecv) : formattedBtcAmount(amountRecv));

        tvFees.setText(switchShowLocalCurrency.isChecked() ?
                formattedFiatAmount(fees) : formattedBtcAmount(fees));

        tvHash.setText(recordTransaction.getHash());

        updatePageChangeBtn();
    }

    private void updatePageChangeBtn() {
        if (idxCurrentPosition < (listRecordTransactions.size() - 1)) {
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
        if (0 == idxCurrentPosition) {
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

    private String formattedBtcAmount(double src) {
        String result = String.format(Locale.getDefault(), "%.8f", src);
        return (result.substring(0, result.length() - 4) + " " +
                result.substring(result.length() - 4));
    }

    private String formattedFiatAmount(double src) {
        NumberFormat formatter = new DecimalFormat("#,###.###");
        return formatter.format(src);
    }

    private void getDatasetFromDB() {
        listRecordTransactions = OpenturnkeyDB.getAllTransactions();

        Collections.sort(listRecordTransactions, new Comparator<RecordTransaction>() {
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                float x2 = event.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    if (x2 > x1) {
                        btnPrevious.callOnClick();
                    } else {
                        btnNext.callOnClick();
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}
