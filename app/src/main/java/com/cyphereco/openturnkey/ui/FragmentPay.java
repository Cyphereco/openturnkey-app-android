package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.blockcypher.exception.BlockCypherException;
import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.UnsignedTx;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkRequest;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.db.RecordTransaction;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.BtcExchangeRates;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.TxFee;
import com.cyphereco.openturnkey.webservices.BlockChainInfo;
import com.cyphereco.openturnkey.webservices.BlockCypher;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class FragmentPay extends FragmentExtendOtkViewPage {

    final String CRLF = "\n";
    public static final String KEY_QR_CODE = "KEY_QR_CODE";

    private boolean signPayment = false;
    private String pubKeyPayer = "";
    private int numOfSignaturesToSign = 0;
    private List<String> listSignatures;

    boolean mUseFixAddress = false;
    boolean mIsCryptoCurrencySet = true;
    private LocalCurrency localCurrency = null;
    private BtcExchangeRates btcExchangeRates;
    private boolean mIsAmountConverting = false;
    private double mBtc = 0.0;

    private EditText tvAmountBtc;
    private EditText tvAmountFiat;
    private TextView tvAddress;
    private TextView tvCurrency;
    private TextView lblEstFees;
    private TextView tvEstFees;
    private TextView lblEstFeesUnit;
    private CheckBox cbUseAllFunds;
    private CheckBox cbAuthByPin;
    private Menu mMenu;
    private Handler xrateHandler;
    private Handler txHandler;
    private Dialog fsDialogSendingBtc;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        listSignatures = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay, container, false);
        // Set not focusable for recipient address so that the hint is shown correctly

        // handler for exchange rate updated
        xrateHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.obj != null) {
                    btcExchangeRates = (BtcExchangeRates) msg.obj;
                    if (tvAmountFiat.getText() != null && tvAmountFiat.getText().toString().contains("-")) {
                        tvAmountFiat.setText("");
                    }
                    convertCurrency();
                }
                updateEstFees();
                return false;
            }
        });

        // handler for tx completed
        txHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                RecordTransaction recordTransaction = (RecordTransaction) msg.obj;
                fsDialogSendingBtc.cancel();

                if (recordTransaction != null) {
                    RecordTransaction item = addTxToDb(recordTransaction);
                    MainActivity.switchToPage(MainActivity.PAGE.HISTORY.ordinal());

                    Intent intent = new Intent(getContext(), ActivityTransactionInfo.class);
                    intent.putExtra(ActivityTransactionInfo.KEY_CURRENT_TRANS_ID, item.getId());
                    if (getActivity() != null) {
                        getActivity().startActivityForResult(intent,
                                MainActivity.REQUEST_CODE_TRANSACTION_INFO);
                    }
                } else {
                    AlertPrompt.alert(getContext(), getString(R.string.send_tx_fail));
                }

                // clear signatures after transaction processed
                listSignatures.clear();
                return false;
            }
        });

        tvAddress = view.findViewById(R.id.input_recipient_address);
        tvAddress.setFocusable(false);
        tvCurrency = view.findViewById(R.id.text_local_currency);

        lblEstFees = view.findViewById(R.id.label_fragment_pay_est_fees);
        tvEstFees = view.findViewById(R.id.tv_fragment_pay_est_fees);
        lblEstFeesUnit = view.findViewById(R.id.label_fragment_pay_est_fees_unit);
        updateEstFees();

        ImageView iv;
        iv = view.findViewById(R.id.icon_scan_qrcode);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchQRcodeScanActivity();
            }
        });

        iv = view.findViewById(R.id.icon_paste_clipboard);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pasteAddressFromClipboard();
            }
        });

        iv = view.findViewById(R.id.icon_read_nfc);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUseFixAddress) {
                    dialogFixAddressEnabled();
                } else {
                    clearRequest();
                    showDialogReadOtk(null, null);
                }
            }
        });

        cbUseAllFunds = view.findViewById(R.id.cb_fragment_pay_use_all_funds);
        cbAuthByPin = view.findViewById(R.id.cb_fragment_pay_auth_by_pin);
        tvAmountBtc = view.findViewById(R.id.tv_fragment_pay_amount_btc);
        tvAmountBtc.setSelectAllOnFocus(true);
        tvAmountFiat = view.findViewById(R.id.tv_fragment_pay_amount_fiat);
        tvAmountFiat.setSelectAllOnFocus(true);
        tvAmountFiat.setText("-.-");
        tvAmountFiat.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && btcExchangeRates == null) {
                    AlertPrompt.alert(getContext(), getString(R.string.could_not_get_exchange_rate));
                    tvAmountFiat.clearFocus();
                    tvAmountBtc.requestFocus();
                }
            }
        });
        updateLocalCurrencyUI();

        MainActivity.addToListOnlineDataUpdateListener(new MainActivity.OnlineDataUpdateListener() {
            @Override
            public void onExchangeRateUpdated(BtcExchangeRates xrate) {
                Message msg = new Message();
                msg.obj = xrate;
                xrateHandler.sendMessage(msg);
            }

            @Override
            public void onTxFeeUpdated(TxFee txFee) {
                Message msg = new Message();
                xrateHandler.sendMessage(msg);
            }

            @Override
            public void onBlockHeightUpdated(long height) {

            }
        });

        iv = view.findViewById(R.id.clear_recipient_address);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Preferences.getUseFixAddressChecked()) {
                    tvAddress.setText("");
                } else {
                    dialogFixAddressEnabled();
                }
            }
        });

        iv = view.findViewById(R.id.clear_amount);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvAmountBtc.setText("");
                tvAmountFiat.setText("");
            }
        });

        tvAmountBtc.addTextChangedListener(new TextWatcherCurrency(tvAmountBtc) {
            @Override
            public void afterTextChanged(Editable editable) {
                if (mIsAmountConverting || !tvAmountBtc.isFocused()) {
                    // It's updated by changing of lc
                    return;
                }
                super.afterTextChanged(editable);
                mIsCryptoCurrencySet = true;
                mIsAmountConverting = true;
                try {
                    double cc = Double.parseDouble(tvAmountBtc.getText().toString());
                    tvAmountFiat.setText(String.format(Locale.ENGLISH, "%.2f", btcToFiat(cc)));
                    mBtc = cc;
                } catch (NumberFormatException e) {
                    tvAmountFiat.setText("");
                }
                mIsAmountConverting = false;
            }
        });

        tvAmountFiat.addTextChangedListener(new TextWatcherCurrency(tvAmountFiat) {
            @Override
            public void afterTextChanged(Editable editable) {
                if (mIsAmountConverting || !tvAmountFiat.isFocused()) {
                    // It's updated by changing of cc or back to this fragment from somewhere
                    return;
                }
                super.afterTextChanged(editable);
                mIsCryptoCurrencySet = false;
                mIsAmountConverting = true;
                try {
                    double lc = Double.parseDouble(tvAmountFiat.getText().toString());
                    mBtc = fiatToBtc(lc);
                    tvAmountBtc.setText(String.format(Locale.ENGLISH, "%.8f", mBtc));

                } catch (NumberFormatException e) {
                    tvAmountBtc.setText("");
                }
                mIsAmountConverting = false;
            }
        });

        // Sign payment button
        Button btn = view.findViewById(R.id.button_sign_payment);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if recipient address is valid.
                try {
                    if (!((MainActivity)getActivity()).isNetworkConnected()) {
                        makeToastMessage(getString(R.string.no_internet));
                        return;
                    }
                    // Check if address is valid
                    if (tvAddress.getText() == null || tvAddress.getText().length() == 0) {
                        makeToastMessage(getString(R.string.recipient_is_empty));
                        return;
                    }
                    if (BtcUtils.isSegWitAddress(!Preferences.isTestnet(), tvAddress.getText().toString())) {
                        makeToastMessage(getString(R.string.seg_wit_address_is_not_supported));
                        return;
                    }
                    if (!BtcUtils.validateAddress(!Preferences.isTestnet(), tvAddress.getText().toString())) {
                        makeToastMessage(getString(R.string.invalid_address));
                        return;
                    }
                    if (!cbUseAllFunds.isChecked() && tvAmountBtc.length() == 0) {
                        makeToastMessage(getString(R.string.amount_empty));
                        return;
                    }
                    if (totalAmountSent() >= 0 && totalAmountReceived() < 0) {
                        makeToastMessage(getString(R.string.amount_less_than_fees));
                        return;
                    }

                    signPayment = true;

                    showDialogReadOtk(getString(R.string.check_opentunrkey_balance), null, new Handler(new Handler.Callback() {
                        @Override
                        public boolean handleMessage(Message msg) {
                            signPayment = false;
                            return false;
                        }
                    }));
                } catch (NullPointerException | NumberFormatException e) {
                    e.printStackTrace();
                    makeToastMessage(getString(R.string.incorrect_recipient_amount));
                }
            }
        });

        cbUseAllFunds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tvAmountBtc.setText("");
                    tvAmountBtc.setEnabled(false);
                    tvAmountFiat.setText("");
                    tvAmountFiat.setEnabled(false);
                } else {
                    tvAmountBtc.setEnabled(true);
                    tvAmountFiat.setEnabled(true);
                }
            }
        });

        // Use All Fund might be checked already, update amount field if it's editable
        if (cbUseAllFunds.isChecked()) {
            tvAmountBtc.setEnabled(false);
            tvAmountFiat.setEnabled(false);
        } else {
            tvAmountBtc.setEnabled(true);
            tvAmountFiat.setEnabled(true);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        pubKeyPayer = "";

        // in case when MainActivity resumed for the first time, no fragment has been selected yet
        // and onPageSelected is not called; call onPageSelected to update UI/Configurations/Dataset
        if (MainActivity.getSelectedFragment() == null) {
            MainActivity.setSelectedFragment(this);
            onPageSelected();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        logger.debug("onActivityResult:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == MainActivity.REQUEST_CODE_QR_CODE) {
            if (resultCode == RESULT_OK) {
                String addr = data.getStringExtra(KEY_QR_CODE);
                logger.info("QR result: {}", addr);

                if (MainActivity.isAddressValid(addr)) {
                    tvAddress.setText(addr);
                } else {
                    if (BtcUtils.isSegWitAddress(!Preferences.isTestnet(), addr)) {
                        AlertPrompt.alert(getContext(), getString(R.string.seg_wit_address_is_not_supported));
                    } else {
                        AlertPrompt.alert(getContext(), getString(R.string.invalid_address));
                    }
                }
            }
        }
    }

    @Override
    public void onPageSelected() {
        super.onPageSelected();

        if (Preferences.getUseFixAddressChecked()) {
            if (MainActivity.getPayToAddress().length() > 0) {
                dialogFixAddressEnabled();
            }
            tvAddress.setText(Preferences.getUseFixAddressAddrString());
            mUseFixAddress = true;
        } else {
            String addr = "";

            if (MainActivity.getPayToAddress().length() > 0) {
                logger.debug("Address from clipboard: {}", MainActivity.getPayToAddress());
                addr = MainActivity.getPayToAddress();
                MainActivity.setPayToAddress("");
            } else if (MainActivity.getQrScanText().length() > 0) {
                logger.debug("Address from QR Scan or NFC read: {}", MainActivity.getQrScanText());
                addr = MainActivity.getQrScanText();
                MainActivity.setQrScanText("");
            }

            if (addr.length() > 0) {
                if (MainActivity.isAddressValid(addr)) {
                    tvAddress.setText(addr);
                } else {
                    AlertPrompt.alert(getContext(), getString(R.string.invalid_address));
                }
            }
        }

        updateLocalCurrencyUI();
        updateEstFees();
    }

    @Override
    public void onPageUnselected() {
        super.onPageUnselected();

        if (!Preferences.getUseFixAddressChecked()) tvAddress.setText("");
        tvAmountBtc.setText("");
        tvAmountFiat.setText("");
        cbUseAllFunds.setChecked(false);
        cbAuthByPin.setChecked(false);
        mBtc = 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int optionId = item.getItemId();

        switch (optionId) {
            case R.id.menu_pay_local_curreny:
                dialogLocalCurrency();
                return true;
            case R.id.menu_pay_transaction_fee:
                dialogTransactionFee();
                return true;
            case R.id.menu_pay_fee_included:
                Preferences.setFeeIncluded(item.setChecked(!item.isChecked()).isChecked());
                updateEstFees();
                return true;
            case R.id.menu_pay_use_fix_address:
                mUseFixAddress = onFixAddressChecked(!item.isChecked());
                item.setChecked(mUseFixAddress);
                Preferences.setUseFixAddress(mUseFixAddress, tvAddress.getText().toString());
                return true;
            case R.id.menu_pay_user_guide:
                String url = "https://openturnkey.com/guide";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                return true;
            case R.id.menu_pay_about:
                dialogAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void preRequestSend(OtkRequest request, OtkData otkData) {
        super.preRequestSend(request, otkData);
        if (pubKeyPayer.length() > 0 && !otkData.getPublicKey().equals(pubKeyPayer)) {
            request.setValid(false);

            dialogReadOtk.updateReadOtkDesc(getString(R.string.incorrect_openturnkey))
                    .endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

            delayProcessAfterReadOtkEnded(new PostReadOtkHandler() {
                @Override
                public void postProcess() {
                    AlertPrompt.alert(getContext(), getString(R.string.incorrect_openturnkey));
                }
            });
        }
    }

    @Override
    public void onOtkDataPosted(final OtkData otkData) {
        super.onOtkDataPosted(otkData);

        // process received otk data
        if (otkData != null) {
            final String addr = otkData.getSessionData().getAddress();

            if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_NA) {
                if (signPayment) {
                    signPayment = false;
                    final String from = otkData.getSessionData().getAddress();
                    final String to = tvAddress.getText().toString();
                    logger.debug("Pay to ({}) for {} / {} (btc/{}) with tx fee ({}) included: {}",
                            to, tvAmountBtc.getText(), tvAmountFiat.getText(),
                            getLocalCurrency().toString(), getTxFees(),
                            Preferences.getFeeIncluded());
                    // prepare to make payment
                    final Dialog dialog = dialogFullscreenAlert(getString(R.string.check_balance));
                    dialog.show();
                    // check balance from payer address
                    logger.debug("checking balance");
                    BlockChainInfo.getBalance(addr, new BlockChainInfo.WebResultHandler() {
                        @Override
                        public void onBalanceUpdated(BigDecimal balance) {
                            logger.debug("Account ({}) balance: {}", otkData.getSessionData().getAddress(), balance);
                            long amountSent = totalAmountSent();
                            long amountReceived = totalAmountReceived();
                            if (amountSent < 0) {
                                amountSent = balance.longValue();
                                amountReceived += amountSent;
                            }
                            dialog.cancel();
                            if (balance.longValue() < amountSent || amountReceived < 0) {
                                // balance not enough
                                // calling AlertPrompt from thread need Looper.prepare
                                AlertPrompt.threadSafeAlert(getContext(), getString(R.string.balance_insufficient));
                            } else {
                                pubKeyPayer = otkData.getPublicKey();
                                // proceed final confirmation
                                logger.debug("Amount sent (include fees): {}, Amount received: {}",
                                        amountSent, amountReceived);
                                dialogConfirmPayment(from, to, amountSent, amountReceived, getTxFees());
                            }
                        }

                        @Override
                        public void onBlockHeightUpdated(long height) {
                        }

                        @Override
                        public void onTxTimeUpdated(long time) {

                        }

                        @Override
                        public void onTxBlockHeightUpdated(long height) {
                        }

                        @Override
                        public void onRawTxUpdated(String rawTx) {
                        }

                        @Override
                        public void onConfirmationsUpdated(long confirmations) {
                        }
                    });
                } else {
                    // read OTK general information, must be getting the OTK address
                    if (MainActivity.isAddressValid(addr)) {
                        tvAddress.setText(addr);
                    } else {
                        AlertPrompt.alert(getContext(), getString(R.string.invalid_address));
                    }
                }
            } else {
                // otkData contains request result, process with proper indications
                if (otkData.getOtkState().getCommand() == Command.SIGN) {
                    if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_SUCCESS) {
                        logger.debug("num of request: {}", numOfRequest());

                        if (otkData.getSessionData().getRequestSigList().size() > 0) {
//                            logger.debug("Got signatures: {}", otkData.getSessionData().getRequestSigList());
                            listSignatures.addAll(otkData.getSessionData().getRequestSigList());
                            logger.debug("listSignatures add signatures: {}", listSignatures);

                            if (hasRequest() && peekRequest().hasMore()) {
                                // request not all processed
                            }
                            else {
                                // all requests processed, send the transaction with signatures
                                fsDialogSendingBtc = dialogFullscreenAlert(getString(R.string.sending_transaction));
                                fsDialogSendingBtc.show();
                                completeTransaction();
                            }
                        } else {
                            // we are expecting signed signatures, something wrong, stop
                            clearRequest();
                            dialogReadOtk.updateReadOtkDesc(getString(R.string.incorrect_openturnkey))
                                    .endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

                            delayProcessAfterReadOtkEnded(new PostReadOtkHandler() {
                                @Override
                                public void postProcess() {
                                    AlertPrompt.alert(getContext(), getString(R.string.communication_error));
                                }
                            });
                        }
                    } else {
                        AlertPrompt.alert(getContext(), getString(R.string.pay_fail) + CRLF
                                + getString(R.string.reason) + ": " +
                                parseFailureReason(otkData.getOtkState().getFailureReason().getValue()));
                    }
                } else {
                    AlertPrompt.alert(getContext(), getString(R.string.communication_error) + CRLF
                            + getString(R.string.reason) + ": " + getString(R.string.not_valid_openturnkey));
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_pay, menu);

        updatePayConfig(menu);
        mMenu = menu;
    }

    private void launchQRcodeScanActivity() {
        if (mUseFixAddress) {
            dialogFixAddressEnabled();
        } else {
            Intent intent = new Intent(getActivity(), ActivityQRcodeScan.class);
            startActivityForResult(intent, MainActivity.REQUEST_CODE_QR_CODE);
        }
    }

    private void pasteAddressFromClipboard() {
        if (mUseFixAddress) {
            dialogFixAddressEnabled();
        } else {
            ClipboardManager clipboard = (ClipboardManager) Objects.requireNonNull(getActivity()).getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
                android.content.ClipData data = clipboard.getPrimaryClip();
                if (data != null && description != null) {
                    String addr = String.valueOf(data.getItemAt(0).coerceToText(getContext()));

                    if (MainActivity.isAddressValid(addr)) {
                        tvAddress.setText(addr);
                    } else {
                        AlertPrompt.alert(getContext(), getString(R.string.invalid_address));
                    }
                }
            }
        }
    }

    private RecordTransaction addTxToDb(RecordTransaction recordTransaction) {
        if (recordTransaction == null) {
            logger.error("addTxToDb(): tx is null");
            return null;
        }
        if (btcExchangeRates != null) recordTransaction.setExchangeRate(btcExchangeRates.toString());
        logger.info("addTxToDb() tx:\n{}", recordTransaction.toString());

        // Get timezone offset
//        Calendar mCalendar = new GregorianCalendar();
//        TimeZone mTimeZone = mCalendar.getTimeZone();
//        int mGMTOffset = mTimeZone.getRawOffset();

        // Add transaction to database.
//        RecordTransaction dbTrans = new RecordTransaction(0,
////                BtcUtils.convertDateTimeStringToLong(transaction.getTime()) + mGMTOffset,
//                transaction.getTimestamp(),
//                transaction.getHash(), transaction.getFrom(), transaction.getTo(), transaction.getAmount(), transaction.getFee(),
//                transaction.getStatus().toInt(), transaction.getDesc(), transaction.getRaw(), transaction.getConfirmations());
        return OpenturnkeyDB.insertTransaction(recordTransaction);
//        logger.info("DB tx count:{}", OpenturnkeyDB.getTransactionCount());
    }

    private void updatePayConfig(Menu menu) {
        String str;
        MenuItem menuItem;

        menuItem = menu.findItem(R.id.menu_pay_local_curreny);
        if (menuItem != null) {
            str = getString(R.string.local_currency) +
                    " (" + getLocalCurrency().toString() + ")";
            menuItem.setTitle(str);
        }

        menuItem = menu.findItem(R.id.menu_pay_transaction_fee);
        Configurations.TxFeeType txFeeType = Preferences.getTxFeeType();
        if (menuItem != null) {
            str = getString(R.string.transaction_fee) +
                    " (" + txFeeToString(txFeeType) + ")";
            menuItem.setTitle(str);
        }

        menuItem = menu.findItem(R.id.menu_pay_fee_included);
        if (menuItem != null) {
            menuItem.setChecked(Preferences.getFeeIncluded());
        }

        menuItem = menu.findItem(R.id.menu_pay_use_fix_address);
        if (menuItem != null) {
            menuItem.setChecked(Preferences.getUseFixAddressChecked());
        }

        updateEstFees();
    }

    private LocalCurrency getLocalCurrency() {
        if (localCurrency == null) {
            localCurrency = Preferences.getLocalCurrency();
        }
        return localCurrency;
    }

    private void updateLocalCurrencyUI() {
        if (this.getContext() == null) return;

        tvCurrency.setText(getLocalCurrency().toString());
        lblEstFeesUnit.setText(getString(R.string._unit_btc) + " / " + getLocalCurrency().toString());

        boolean cache = mIsCryptoCurrencySet;
        mIsCryptoCurrencySet = true;
        convertCurrency();
        mIsCryptoCurrencySet = cache;
    }

    private boolean onFixAddressChecked(boolean isChecked) {
        if (isChecked) {
            // If address editor is empty, prompt warning dialog
            if ((null == tvAddress.getText()) || (tvAddress.getText().toString().isEmpty())) {
                mUseFixAddress = false;
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.recipient_is_empty))
                        .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                return false;
            }
            // There is address
            Preferences.setUseFixAddress(true, tvAddress.getText().toString());
            mUseFixAddress = true;
            return true;
        }
        mUseFixAddress = false;
        return false;
    }

    private LocalCurrency optionToLocalCurrency(int currency) {
        switch (currency) {
            case R.id.radio_cny:
                return LocalCurrency.LOCAL_CURRENCY_CNY;
            case R.id.radio_eur:
                return LocalCurrency.LOCAL_CURRENCY_EUR;
            case R.id.radio_jpy:
                return LocalCurrency.LOCAL_CURRENCY_JPY;
            case R.id.radio_twd:
                return LocalCurrency.LOCAL_CURRENCY_TWD;
            default:
                return LocalCurrency.LOCAL_CURRENCY_USD;
        }
    }

    private Configurations.TxFeeType optionToTxFeeType(int option) {
        switch (option) {
            case R.id.radio_custom:
                return Configurations.TxFeeType.CUSTOMIZED;
            case R.id.radio_high:
                return Configurations.TxFeeType.HIGH;
            case R.id.radio_mid:
                return Configurations.TxFeeType.MID;
            default:
                return Configurations.TxFeeType.LOW;
        }
    }

    private String txFeeToString(Configurations.TxFeeType txFeeType) {
        if (txFeeType == Configurations.TxFeeType.CUSTOMIZED) {
            return getString(R.string.customized_fees);
        }
        if (txFeeType == Configurations.TxFeeType.HIGH) {
            return getString(R.string.fees_high);
        }
        if (txFeeType == Configurations.TxFeeType.MID) {
            return getString(R.string.fees_mid);
        }
        return getString(R.string.fees_low);
    }

    private double btcToFiat(double amount) {
        if (amount > 0 && btcExchangeRates != null) {
            switch (getLocalCurrency()) {
                case LOCAL_CURRENCY_CNY:
                    return (btcExchangeRates.getRate_cny() * amount);
                case LOCAL_CURRENCY_EUR:
                    return (btcExchangeRates.getRate_eur() * amount);
                case LOCAL_CURRENCY_JPY:
                    return (btcExchangeRates.getRate_jpy() * amount);
                case LOCAL_CURRENCY_TWD:
                    return (btcExchangeRates.getRate_twd() * amount);
                case LOCAL_CURRENCY_USD:
                    return (btcExchangeRates.getRate_usd() * amount);
            }
        }

        return 0;
    }

    private double fiatToBtc(double lc) {
        if (btcExchangeRates == null) {
            return 0;
        }
        switch (getLocalCurrency()) {
            case LOCAL_CURRENCY_CNY:
                return lc / btcExchangeRates.getRate_cny();
            case LOCAL_CURRENCY_EUR:
                return lc / btcExchangeRates.getRate_eur();
            case LOCAL_CURRENCY_JPY:
                return lc / btcExchangeRates.getRate_jpy();
            case LOCAL_CURRENCY_TWD:
                return lc / btcExchangeRates.getRate_twd();
            case LOCAL_CURRENCY_USD:
                return lc / btcExchangeRates.getRate_usd();
        }
        return 0;
    }

    private void convertCurrency() {
        try {
            if (mIsCryptoCurrencySet) {
                if (mBtc == 0.0) {
                    // No need to update if amount is 0.
                    return;
                }
                // update local currency
                mIsAmountConverting = true;
                try {
                    tvAmountFiat.setText(String.format(Locale.ENGLISH, "%.2f", btcToFiat(mBtc)));
                } catch (NumberFormatException e) {
                    tvAmountFiat.setText("");
                }
                mIsAmountConverting = false;
            } else {
                mIsAmountConverting = true;
                try {
                    double lc = Double.parseDouble(tvAmountFiat.getText().toString());
                    mBtc = fiatToBtc(lc);
                    tvAmountBtc.setText(String.format(Locale.ENGLISH, "%.8f", mBtc));
                } catch (NumberFormatException e) {
                    tvAmountBtc.setText("");
                }
                mIsAmountConverting = false;
            }
        } catch (NumberFormatException e) {
            // do nothing e.printStackTrace();
        } catch (NullPointerException e) {
            // Do nothing
        }
    }

    private long getTxFees() {
        switch (Preferences.getTxFeeType()) {
            case HIGH:
                return Preferences.getTxFee().getHigh() * 200;
            case MID:
                return Preferences.getTxFee().getMid() * 200;
            case LOW:
                return Preferences.getTxFee().getLow() * 200;
            case CUSTOMIZED:
                return Preferences.getCustomizedTxFee();
        }
        return 1000;
    }

    private long totalAmountSent() {
        if (cbUseAllFunds.isChecked()) return -1;

        long total = 0;
        if (tvAmountBtc.getText().length() != 0) {
            total = (long) (Double.parseDouble(tvAmountBtc.getText().toString()) * 100000000);
        }
        if (!Preferences.getFeeIncluded()) {
            total += getTxFees();
        }
        return total;
    }

    private long totalAmountReceived() {
        if (cbUseAllFunds.isChecked()) return -(getTxFees());

        long total = 0;
        if (tvAmountBtc.getText().length() != 0) {
            total = (long) (Double.parseDouble(tvAmountBtc.getText().toString()) * 100000000);
        }
        if (Preferences.getFeeIncluded()) {
            total -= getTxFees();
        }
        return total;
    }

    private void paymentConfirmed(final String from, final String to,
                                  final long amountReceived, final long fees) {
        paymentConfirmed(from, to, amountReceived, fees, null);
    }

    private void paymentConfirmed(final String from, final String to,
                                  final long amountReceived, final long fees, final  String pin) {
        logger.debug("payment confirmed");

        final Dialog dialog = dialogFullscreenAlert(getString(R.string.create_transaction));
        final Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                dialog.cancel();
                if (msg.obj == null) {
                    AlertPrompt.alert(getContext(), getString(R.string.error_cannot_create_tx));
                } else {
                    logger.debug("Unsigned Tx created: {}", msg.obj.toString());
                    UnsignedTx unsignedTx = (UnsignedTx) msg.obj;
                    // create OTK request for signatures
                    List list = unsignedTx.getToSign();
                    numOfSignaturesToSign = list.size();
                    StringBuilder hashes = new StringBuilder();
                    int hashesCounter = 0;
                    for (int i = 0; i < list.size(); i++) {
                        hashes.append(hashes.length() > 0 ? "\n" : "").append(list.get(i));
                        hashesCounter++;

                        if (hashesCounter > 9) {
                            OtkRequest request = new OtkRequest(Command.SIGN.toString(), hashes.toString()).setPin(pin);
                            // A request with more=1 has been set, follow the same reuqest ID (Not necessarily but may be better for future)
                            if (hasRequest() && peekRequest().hasMore()) request.setRequestId(peekRequest().getRequestId());
                            // signatures not fully processed, set more=1
                            if (i + 1 < list.size()) request.setMore(true);
                            pushRequest(request);
                            hashes = new StringBuilder();
                            hashesCounter = 0;
                        }
                    }
                    if (hashesCounter > 0) {
                        // there are signatures (< 10) not added to request,  add them now
                        OtkRequest request = new OtkRequest(Command.SIGN.toString(), hashes.toString()).setPin(pin);
                        if (hasRequest() && peekRequest().hasMore()) request.setRequestId(peekRequest().getRequestId());
                        pushRequest(request);
                    }
                    logger.debug("Request Number: {}", numOfRequest());

                    showDialogReadOtk(getString(R.string.signing_transaction), null);
                }
                return false;
            }
        });
        new Thread() {
            @Override
            public void run() {
                super.run();
                Message msg = new Message();
                try {
                    if (cbUseAllFunds.isChecked()) {
                        msg.obj = BlockCypher.newTransaction(from, to, -1, fees);
                    }
                    else {
                        msg.obj = BlockCypher.newTransaction(from, to, amountReceived, fees);
                    }
                } catch (BlockCypherException e) {
                    logger.error("Error: {}",
                            BlockCypher.parseError(e.getBlockCypherError().getErrors().get(0).toString()));
                } catch (Exception e) {
                    logger.error("Error: {}", e.toString());
                }
                handler.sendMessage(msg);
            }
        }.start();
        dialog.show();
    }

    private void completeTransaction() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        Message msg = new Message();
//                        msg.obj = _newyDummyTransaction();
                        logger.debug("completeTransaction: Got listSignatures ({})", listSignatures.size());
                        msg.obj = BlockCypher.completeSendBitcoin(pubKeyPayer, listSignatures, tvAddress.getText().toString());
                        logger.debug("Sent transaction output: {})", msg.obj);
                        txHandler.sendMessage(msg);
                    }
//                    catch (BlockCypherException e) {
//                        // parse error
//                        String reason = "";
//                        if (e != null && e.getBlockCypherError() != null && e.getBlockCypherError().getErrors() != null) {
//                            reason = BlockCypher.parseError(e.getBlockCypherError().getErrors().get(0).toString());
//                        } else if (e.getMessage() != null) {
//                            reason = e.getMessage();
//                        }
//                        logger.error("Error: {}", reason);
//                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void dialogConfirmPayment(final String from, final String to, long amountSent, final long amountReceived, final long fees) {
        // preparing confirmation context
        long estBlocks = BtcUtils.getEstimatedTime(fees);
        String estTime = (estBlocks == 1) ? " 5~15" : ((estBlocks > 3) ? " 40~60+" : " 15~35");
        double btcTxfees = BtcUtils.satoshiToBtc(fees);
        double btcAmountSent = BtcUtils.satoshiToBtc(amountSent);
        double btcAmountRecv = BtcUtils.satoshiToBtc(amountReceived);

        LocalCurrency lc = getLocalCurrency();

        // format amount Strings
        String strBtcAmountSent = String.format(Locale.ENGLISH, "%.8f", btcAmountSent);
        String strFiatAmountSent = String.format(Locale.ENGLISH, "%.3f", BtcUtils.btcToLocalCurrency(btcExchangeRates, lc, btcAmountSent));
        String strBtcAmountRecv = String.format(Locale.ENGLISH, "%.8f", btcAmountRecv);
        String strFiatAmountRecv = String.format(Locale.ENGLISH, "%.3f", BtcUtils.btcToLocalCurrency(btcExchangeRates, lc, btcAmountRecv));
        String strBtcFees = String.format(Locale.ENGLISH, "%.8f", btcTxfees);
        String strFiatFess = String.format(Locale.ENGLISH, "%.3f", BtcUtils.btcToLocalCurrency(btcExchangeRates, lc, btcTxfees));

        // confirmation summary string
        String msg = getString(R.string.subject_text_estimated_time) + " " + estTime + " " + getString(R.string.minute) + CRLF + CRLF +
                getString(R.string.amount_sent) + CRLF + strBtcAmountSent + " / " +
                strFiatAmountSent + " (" + getString(R.string._unit_btc) + "/" + lc.toString() + ")" + CRLF +
                getString(R.string.sender) + ":" + CRLF + from + CRLF + CRLF +
                getString(R.string.amount_received) + CRLF + strBtcAmountRecv + " / " +
                strFiatAmountRecv + " (" + getString(R.string._unit_btc) + "/" + lc.toString() + ")" + CRLF +
                getString(R.string.recipient) + ":" + CRLF + to + CRLF + CRLF +
                getString(R.string.transaction_fee) + ":" + CRLF + strBtcFees + " / " +
                strFiatFess + " (" + getString(R.string._unit_btc) + "/" + lc.toString() + ")";

        if (Looper.myLooper() == null) Looper.prepare();
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_payment)
                .setMessage(msg)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger.debug("canceled payment on confirmation");
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (cbAuthByPin.isChecked()) {
                            DialogAuthByPin dialogAuthByPin = new DialogAuthByPin();
                            dialogAuthByPin.setListener(new DialogAuthByPin.DialogAuthByPinListener() {
                                @Override
                                public void setPin(String pin) {
                                    paymentConfirmed(from, to, amountReceived, fees, pin);
                                }
                            });
                            dialogAuthByPin.show(getFragmentManager(), "AuthByPin");
                            cbAuthByPin.setChecked(false);
                        }
                        else {
                            paymentConfirmed(from, to , amountReceived, fees, null);
                        }
                    }
                })
                .setCancelable(true)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        Looper.loop();
    }

    private void dialogFixAddressEnabled() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.cannot_change_addr)
                .setMessage(R.string.disable_fix_addr_first)
                .setNegativeButton(R.string.ok, null)
                .show();
        MainActivity.setPayToAddress("");
    }

    private void dialogLocalCurrency() {
        DialogLocalCurrency dialog = new DialogLocalCurrency();
        int localCurrencyId;

        LocalCurrency lc = getLocalCurrency();
        if (lc == LocalCurrency.LOCAL_CURRENCY_CNY) {
            localCurrencyId = R.id.radio_cny;
        } else if (lc == LocalCurrency.LOCAL_CURRENCY_EUR) {
            localCurrencyId = R.id.radio_eur;
        } else if (lc == LocalCurrency.LOCAL_CURRENCY_JPY) {
            localCurrencyId = R.id.radio_jpy;
        } else if (lc == LocalCurrency.LOCAL_CURRENCY_USD) {
            localCurrencyId = R.id.radio_usd;
        } else {
            localCurrencyId = R.id.radio_twd;
        }

        // reset local currency to null, will be updated later
        localCurrency = null;

        Bundle bundle = new Bundle();
        bundle.putInt("localCurrency", localCurrencyId);
        dialog.setArguments(bundle);
        dialog.setDialogLocalCurrecyListener(new DialogLocalCurrency.DialogLocalCurrecyListener() {
            @Override
            public void setLocalCurrency(int option) {
                LocalCurrency lc = optionToLocalCurrency(option);
                Preferences.setLocalCurrency(lc);
                updateLocalCurrencyUI();
                updatePayConfig(mMenu);
            }
        });
        assert getFragmentManager() != null;
        dialog.show(getFragmentManager(), "dialog");
    }

    private void dialogTransactionFee() {
        DialogTransactionFee dialog = new DialogTransactionFee();

        Configurations.TxFeeType txFeeType = Preferences.getTxFeeType();
        int transactionFeeId;
        if (txFeeType == Configurations.TxFeeType.CUSTOMIZED) {
            transactionFeeId = R.id.radio_custom;
        } else if (txFeeType == Configurations.TxFeeType.HIGH) {
            transactionFeeId = R.id.radio_high;
        } else if (txFeeType == Configurations.TxFeeType.MID) {
            transactionFeeId = R.id.radio_mid;
        } else {
            transactionFeeId = R.id.radio_low;
        }

        Bundle bundle = new Bundle();
        bundle.putInt("transactionFee", transactionFeeId);
        bundle.putLong("customizedFee", Preferences.getCustomizedTxFee());
        dialog.setArguments(bundle);
        dialog.setDialogTransactionFeeListener(new DialogTransactionFee.DialogTransactionFeeListener() {
            @Override
            public void setTransactionFee(int transactionFee) {
                Preferences.setTxFeeType(optionToTxFeeType(transactionFee));
                updatePayConfig(mMenu);
            }

            @Override
            public void setCustomizedTxFee(double txFee) {
                Preferences.setCustomizedTxFee(BtcUtils.btcToSatoshi(txFee));
                updatePayConfig(mMenu);
            }
        });
        assert getFragmentManager() != null;
        dialog.show(getFragmentManager(), "dialog");
    }

    private void dialogAbout() {
        DialogAbout dialog = new DialogAbout();
        assert getFragmentManager() != null;
        dialog.show(getFragmentManager(), "dialog");
    }

    private Dialog dialogFullscreenAlert(String msg) {
        TextView tv = new TextView(getContext());
        tv.setText(msg);
        tv.setTextSize(24);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        Dialog dialog = builder.setView(tv).create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private void makeToastMessage(String message) {
        Snackbar snackbar = Snackbar.make(Objects.requireNonNull(getView()), message, Snackbar.LENGTH_LONG);
        TextView textView = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextSize(22);
        snackbar.setAction("Action", null).show();
    }

    // add dummy transaction record
    private RecordTransaction _newyDummyTransaction() {
        RecordTransaction recordTransaction = new RecordTransaction();

        // add dummy record for test
        if (cbUseAllFunds.isChecked()) {
            recordTransaction.setTimestamp(1584633269000L);
            recordTransaction.setHash("c92faf592e028f0d3d50203a4119022bfb8f60995cad1d138d48a9fa67c38cdb");
            recordTransaction.setPayer("1A8Ukx5NEytHdnGHRZejmSMaU5md6k4HRN");
            recordTransaction.setPayee("1QEma6prBJscNqw7s3t8EGFcx3zF7mzWab");
            recordTransaction.setAmountSent(0.00168139d);
            recordTransaction.setAmountRecv(0.00167739d);
            recordTransaction.setRawData("010000000110b1373bddada5b32bf4e958694cf767a3ecc104aa3d7f50f5ecf2f91ea6bf75000000006b4830450221009df2ee983117010ddda121eef54aa91edbe1963200f821a19d48c6a4e7c58f2502201cd9508ca99e5f9f54a599d32a589bfe9db527cd527c979656bfca28e79bb7540121034ded0e5cd55e848c946430fc8eb32b445d7650f4e77bee4ca5dd3c358d1dbcb7ffffffff013b8f0200000000001976a914fee5819b32e8618699ad07a17b3df5a77346261788ac00000000");
            recordTransaction.setBlockHeight(-1);
            recordTransaction.setExchangeRate(MainActivity.getBtcExchangeRates().toString());
        }
        else {
            recordTransaction.setTimestamp(1578754831000L);
            recordTransaction.setHash("5dc7bee70b2d4d486d2e9ca997354e6909769049b2d971dc4034e2c03df909c7");
            recordTransaction.setPayer("1QEma6prBJscNqw7s3t8EGFcx3zF7mzWab");
            recordTransaction.setPayee("1QEma6prBJscNqw7s3t8EGFcx3zF7mzWab");
            recordTransaction.setAmountSent(0.00192767d);
            recordTransaction.setAmountRecv(0.00191967d);
            recordTransaction.setRawData("01000000030781f8fa7f6a30621c29ac47a2d5bb81bef88973839680f8f5de0d879c6417f9000000006a47304402204688a19b3ebe5bb05ff3fa05177f6f4889016c29a12ee9abdf325c5e1f32fe1e02205be7f1afa0df30c8165e4de6743f1b034d9de2cf7571f9500d7ce81c2bd9a55d01210323c012252f1f00996c6f05b074b99f516c1a9a0c8966cb645f9a01a11b9fc229fffffffff585248874c9b15176863d579379a5cc2c01453926395973da9264022ec3ed39010000006b483045022100baa9783aedc0b9860e0f486c09bdfbad92f71a5d383019cfcac35ea8ed59f282022030bde9f23990d434e1544e5e1bf227c66a0a2c1aed39e55bb8129385f127e70501210323c012252f1f00996c6f05b074b99f516c1a9a0c8966cb645f9a01a11b9fc229fffffffff585248874c9b15176863d579379a5cc2c01453926395973da9264022ec3ed39000000006a47304402201d6aa95358825ef7319c4a5f1ee89c864c3b71b45926610a9fc95574ca39bff20220459a21b1321687237bc9f37a17df6a0290a0920979882108e816cbd246ac679f01210323c012252f1f00996c6f05b074b99f516c1a9a0c8966cb645f9a01a11b9fc229ffffffff01dfed0200000000001976a914fee5819b32e8618699ad07a17b3df5a77346261788ac00000000");
            recordTransaction.setBlockHeight(612369);
            recordTransaction.setExchangeRate(MainActivity.getBtcExchangeRates().toString());
        }
        return recordTransaction;
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

    private String strTxFeeType(Configurations.TxFeeType type) {
        switch (type) {
            case CUSTOMIZED:
                return getString(R.string.customized);
            case LOW:
                return getString(R.string.low);
            case MID:
                return getString(R.string.mid);
            default:
                return getString(R.string.high);
        }
    }

    private void updateEstFees() {
        if (this.getContext() == null) return;;

        String feesInc = getString(R.string.excluded);
        if(Preferences.getFeeIncluded()) feesInc = getString(R.string.included);

        lblEstFees.setText(getString(R.string.estimated_fees) + " (" +
                strTxFeeType(Preferences.getTxFeeType()) + " - " + feesInc + "):");
        tvEstFees.setText(formattedBtcAmount(getTxFees() / 100000000d)  + " / " +
                formattedFiatAmount(BtcUtils.btcToLocalCurrency(btcExchangeRates,
                        getLocalCurrency(), getTxFees() / 100000000d)));
        lblEstFeesUnit.setText(getString(R.string._unit_btc) + " / " + getLocalCurrency().toString());
    }
}

