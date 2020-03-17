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
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.ExchangeRate;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.TxFee;
import com.cyphereco.openturnkey.webservices.BlockChainInfo;
import com.cyphereco.openturnkey.webservices.BlockCypher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class FragmentPay extends FragmentExtendOtkViewPage {

    final String CRLF = "\n";
    public static final String KEY_QR_CODE = "KEY_QR_CODE";

    private boolean signPayment = false;

    boolean mUseFixAddress = false;
    boolean mIsCryptoCurrencySet = true;
    private LocalCurrency mLocalCurrency = LocalCurrency.LOCAL_CURRENCY_USD;
    private ExchangeRate exchangeRate;
    private boolean mIsAmountConverting = false;
    private double mBtc = 0.0;

    private EditText mEtCc;
    private EditText mEtLc;
    private TextView tvAddress;
    private TextView tvCurrency;
    private CheckBox cbUseAllFunds;
    private Menu mMenu;
    private Handler handler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay, container, false);
        // Set not focusable for recipient address so that the hint is shown correctly

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                mEtLc.setText("");
                exchangeRate = (ExchangeRate) msg.obj;
                convertCurrency();
                return false;
            }
        });

        tvAddress = view.findViewById(R.id.input_recipient_address);
        tvAddress.setFocusable(false);
        tvCurrency = view.findViewById(R.id.text_local_currency);

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
                    DialogReadOtk dialogReadOtk = new DialogReadOtk();
                    assert getFragmentManager() != null;
                    dialogReadOtk.show(getFragmentManager(), "ReadOtk");
                }
            }
        });

        cbUseAllFunds = view.findViewById(R.id.checkBox_use_all_funds);
        mEtCc = view.findViewById(R.id.input_crypto_currency);
        mEtCc.setSelectAllOnFocus(true);
        mEtLc = view.findViewById(R.id.input_local_currency);
        mEtLc.setSelectAllOnFocus(true);
        mEtLc.setText("-.-");
        mEtLc.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && exchangeRate == null) {
                    AlertPrompt.alert(getContext(), "Could not get exchange rate.");
                    mEtLc.clearFocus();
                    mEtCc.requestFocus();
                }
            }
        });
        updateLocalCurrency(Preferences.getLocalCurrency());

        MainActivity.setOnlineDataUpdateListner(new MainActivity.OnlineDataUpdateListener() {
            @Override
            public void onExchangeRateUpdated(ExchangeRate xrate) {
                Message msg = new Message();
                msg.obj = xrate;
                handler.sendMessage(msg);
            }

            @Override
            public void onTxFeeUpdated(TxFee txFee) {

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
                mEtCc.setText("");
                mEtLc.setText("");
            }
        });

        mEtCc.addTextChangedListener(new TextWatcherCurrency(mEtCc) {
            @Override
            public void afterTextChanged(Editable editable) {
                if (mIsAmountConverting || !mEtCc.isFocused()) {
                    // It's updated by changing of lc
                    return;
                }
                super.afterTextChanged(editable);
                mIsCryptoCurrencySet = true;
                mIsAmountConverting = true;
                try {
                    double cc = Double.parseDouble(mEtCc.getText().toString());
                    mEtLc.setText(String.format(Locale.ENGLISH, "%.2f", btcToFiat(cc)));
                    mBtc = cc;
                } catch (NumberFormatException e) {
                    mEtLc.setText("");
                }
                mIsAmountConverting = false;
            }
        });

        mEtLc.addTextChangedListener(new TextWatcherCurrency(mEtLc) {
            @Override
            public void afterTextChanged(Editable editable) {
                if (mIsAmountConverting || !mEtLc.isFocused()) {
                    // It's updated by changing of cc or back to this fragment from somewhere
                    return;
                }
                super.afterTextChanged(editable);
                mIsCryptoCurrencySet = false;
                mIsAmountConverting = true;
                try {
                    double lc = Double.parseDouble(mEtLc.getText().toString());
                    mBtc = fiatToBtc(lc);
                    mEtCc.setText(String.format(Locale.ENGLISH, "%.8f", mBtc));

                } catch (NumberFormatException e) {
                    mEtCc.setText("");
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
                    if (!cbUseAllFunds.isChecked() && mEtCc.length() == 0) {
                        makeToastMessage(getString(R.string.amount_empty));
                        return;
                    }
                    if (totalAmountSent() >= 0 && totalAmountReceived() < 0) {
                        makeToastMessage(getString(R.string.amount_less_than_fees));
                        return;
                    }

                    signPayment = true;

                    DialogReadOtk dialogReadOtk = new DialogReadOtk();
                    assert getFragmentManager() != null;
                    dialogReadOtk.show(getFragmentManager(), "ReadOtk");
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
                    mEtCc.setText("");
                    mEtCc.setEnabled(false);
                    mEtLc.setText("");
                    mEtLc.setEnabled(false);
                } else {
                    mEtCc.setEnabled(true);
                    mEtLc.setEnabled(true);
                }
            }
        });

        // Use All Fund might be checked already, update amount field if it's editable
        if (cbUseAllFunds.isChecked()) {
            mEtCc.setEnabled(false);
            mEtLc.setEnabled(false);
        } else {
            mEtCc.setEnabled(true);
            mEtLc.setEnabled(true);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
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

        mLocalCurrency = Preferences.getLocalCurrency();
        tvCurrency.setText(mLocalCurrency.toString());
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
                return true;
            case R.id.menu_pay_use_fix_address:
                mUseFixAddress = onFixAddressOption(!item.isChecked());
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
    public void onOtkDataPosted(final OtkData otkData) {
        super.onOtkDataPosted(otkData);

        // process received otk data
        if (otkData != null) {
            String addr = otkData.getSessionData().getAddress();

            if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_NA) {
                if (signPayment) {
                    final String from = otkData.getSessionData().getAddress();
                    final String to = tvAddress.getText().toString();
                    logger.debug("Pay to ({}) for {} / {} (btc/{}) with tx fee ({}) included: {}",
                            to, mEtCc.getText(), mEtLc.getText(),
                            Preferences.getLocalCurrency().toString(), getFees(),
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
                                // construct transaction in the background

                                // proceed final confirmation
                                logger.debug("Amount sent (include fees): {}, Amount received: {}",
                                        amountSent, amountReceived);
                                dialogConfirmPayment(from, to, amountSent, amountReceived, getFees());
                            }
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
                    // construct transaction
                    // show confirmation dialog
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
                        // got signatures, prompt a processing dialog
                    } else {
                        AlertPrompt.alert(getContext(), "Pay fail" + CRLF
                                + getString(R.string.reason) + ": " + otkData.getFailureReason());
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

    public void launchQRcodeScanActivity() {
        if (mUseFixAddress) {
            dialogFixAddressEnabled();
        } else {
            Intent intent = new Intent(getActivity(), ActivityQRcodeScan.class);
            startActivityForResult(intent, MainActivity.REQUEST_CODE_QR_CODE);
        }
    }

    public void pasteAddressFromClipboard() {
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

    private void updatePayConfig(Menu menu) {
        String str;
        MenuItem menuItem;

        menuItem = menu.findItem(R.id.menu_pay_local_curreny);
        LocalCurrency lc = Preferences.getLocalCurrency();
        if (menuItem != null) {
            str = getString(R.string.local_currency) +
                    " (" + lc.toString() + ")";
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
    }

    public void updateLocalCurrency(LocalCurrency localCurrency) {
        mLocalCurrency = localCurrency;
        tvCurrency.setText(mLocalCurrency.toString());
        boolean cache = mIsCryptoCurrencySet;
        mIsCryptoCurrencySet = true;
        convertCurrency();
        mIsCryptoCurrencySet = cache;
    }

    private boolean onFixAddressOption(boolean isChecked) {
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

    private Configurations.TxFeeType toTxFee(int transactionFee) {
        switch (transactionFee) {
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

    public LocalCurrency optionToLocalCurrency(int currency) {
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

    private double btcToFiat(double amount) {
        if (amount > 0 && exchangeRate != null) {
            switch (mLocalCurrency) {
                case LOCAL_CURRENCY_TWD:
                    return (exchangeRate.getTWD() * amount);
                case LOCAL_CURRENCY_USD:
                    return (exchangeRate.getUSD() * amount);
                case LOCAL_CURRENCY_CNY:
                    return (exchangeRate.getCNY() * amount);
                case LOCAL_CURRENCY_EUR:
                    return (exchangeRate.getEUR() * amount);
                case LOCAL_CURRENCY_JPY:
                    return (exchangeRate.getJPY() * amount);
            }
        }

        return 0;
    }

    private double fiatToBtc(double lc) {
        if (exchangeRate == null) {
            return 0;
        }
        switch (mLocalCurrency) {
            case LOCAL_CURRENCY_TWD:
                return lc / exchangeRate.getTWD();
            case LOCAL_CURRENCY_USD:
                return lc / exchangeRate.getUSD();
            case LOCAL_CURRENCY_CNY:
                return lc / exchangeRate.getCNY();
            case LOCAL_CURRENCY_EUR:
                return lc / exchangeRate.getEUR();
            case LOCAL_CURRENCY_JPY:
                return lc / exchangeRate.getJPY();
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
                    mEtLc.setText(String.format(Locale.ENGLISH, "%.2f", btcToFiat(mBtc)));
                } catch (NumberFormatException e) {
                    mEtLc.setText("");
                }
                mIsAmountConverting = false;
            } else {
                mIsAmountConverting = true;
                try {
                    double lc = Double.parseDouble(mEtLc.getText().toString());
                    mBtc = fiatToBtc(lc);
                    mEtCc.setText(String.format(Locale.ENGLISH, "%.8f", mBtc));
                } catch (NumberFormatException e) {
                    mEtCc.setText("");
                }
                mIsAmountConverting = false;
            }
        } catch (NumberFormatException e) {
            // do nothing e.printStackTrace();
        } catch (NullPointerException e) {
            // Do nothing
        }
    }

    private long getFees() {
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
        if (mEtCc.getText().length() != 0) {
            total = (long) (Double.parseDouble(mEtCc.getText().toString()) * 100000000);
        }
        if (!Preferences.getFeeIncluded()) {
            total += getFees();
        }
        return total;
    }

    private long totalAmountReceived() {
        if (cbUseAllFunds.isChecked()) return -(getFees());

        long total = 0;
        if (mEtCc.getText().length() != 0) {
            total = (long) (Double.parseDouble(mEtCc.getText().toString()) * 100000000);
        }
        if (Preferences.getFeeIncluded()) {
            total -= getFees();
        }
        return total;
    }

    private void paymentConfirmed(final String from, final String to, final long amountReceived, final long fees) {
        logger.debug("payment confirmed");

        final Dialog dialog = dialogFullscreenAlert(getString(R.string.processing));
        final Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                dialog.cancel();
                logger.debug("Unsigned Tx created: {}", msg.obj.toString());
                if (msg.obj == null) {
                    AlertPrompt.threadSafeAlert(getContext(), getString(R.string.error_cannot_create_tx));
                } else {
                    UnsignedTx unsignedTx = (UnsignedTx) msg.obj;
                    // create OTK request for signatures
                    List list = unsignedTx.getToSign();
                    for (int i = 0; i < list.size(); i++) {
                        logger.debug("{}", list.get(i));
                    }
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
                    msg.obj = BlockCypher.newTransaction(from, to, amountReceived, fees);
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

    private void makeToastMessage(String message) {
        Snackbar snackbar = Snackbar.make(Objects.requireNonNull(getView()), message, Snackbar.LENGTH_LONG);
        TextView textView = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextSize(22);
        snackbar.setAction("Action", null).show();
    }

    private void dialogFixAddressEnabled() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.cannot_change_addr)
                .setMessage(R.string.disable_fix_addr_first)
                .setNegativeButton(R.string.ok, null)
                .show();
        MainActivity.setPayToAddress("");
    }

    private void dialogConfirmPayment(final String from, final String to, long amountSent, final long amountReceived, final long fees) {
        // preparing confirmation context
        long estBlocks = BtcUtils.getEstimatedTime(fees);
        String estTime = (estBlocks == 1) ? " 5~15" : ((estBlocks > 3) ? " 40~60+" : " 15~35");
        double btcTxfees = BtcUtils.satoshiToBtc(fees);
        double btcAmountSent = BtcUtils.satoshiToBtc(amountSent);
        double btcAmountRecv = BtcUtils.satoshiToBtc(amountReceived);

        LocalCurrency lc = Preferences.getLocalCurrency();

        // format amount Strings
        String strBtcAmountSent = String.format(Locale.ENGLISH, "%.8f", btcAmountSent);
        String strFiatAmountSent = String.format(Locale.ENGLISH, "%.3f", BtcUtils.btcToLocalCurrency(exchangeRate, lc, btcAmountSent));
        String strBtcAmountRecv = String.format(Locale.ENGLISH, "%.8f", btcAmountRecv);
        String strFiatAmountRecv = String.format(Locale.ENGLISH, "%.3f", BtcUtils.btcToLocalCurrency(exchangeRate, lc, btcAmountRecv));
        String strBtcFees = String.format(Locale.ENGLISH, "%.8f", btcTxfees);
        String strFiatFess = String.format(Locale.ENGLISH, "%.3f", BtcUtils.btcToLocalCurrency(exchangeRate, lc, btcTxfees));

        // confirmation summary string
        String msg = getString(R.string.subject_text_estimated_time) + estTime + CRLF + CRLF +
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
                        paymentConfirmed(from, to, amountReceived, fees);
                    }
                })
                .setCancelable(true)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        Looper.loop();
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

    private void dialogLocalCurrency() {
        DialogLocalCurrency dialog = new DialogLocalCurrency();
        int localCurrencyId;

        LocalCurrency lc = Preferences.getLocalCurrency();
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
        Bundle bundle = new Bundle();
        bundle.putInt("localCurrency", localCurrencyId);
        dialog.setArguments(bundle);
        dialog.setDialogLocalCurrecyListener(new DialogLocalCurrency.DialogLocalCurrecyListener() {
            @Override
            public void setLocalCurrency(int localCurrency) {
                LocalCurrency lc = optionToLocalCurrency(localCurrency);
                Preferences.setLocalCurrency(lc);
                updateLocalCurrency(lc);
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
                Preferences.setTxFeeType(toTxFee(transactionFee));
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
}

