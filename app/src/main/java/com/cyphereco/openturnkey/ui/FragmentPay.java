package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.Editable;
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

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.ExchangeRate;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.TxFee;

import java.util.Locale;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class FragmentPay extends FragmentExtendOtkViewPage {

    public static final String KEY_QR_CODE = "KEY_QR_CODE";

    private boolean setPayer = false;
    private String addrPayer = null;

    boolean mUseFixAddress = false;
    boolean mIsCryptoCurrencySet = true;
    private LocalCurrency mLocalCurrency = LocalCurrency.LOCAL_CURRENCY_USD;
    private ExchangeRate objExchangeRate;
    private boolean mIsAmountConverting = false;
    private double mBtc = 0.0;
    private static final String ARG_TO = "TO";
    private static final String ARG_BTC = "BTC";
    private static final String ARG_LC = "LC";
    private static final String ARG_USE_ALL_FUNDS = "USE_ALL_FUNDS";

    private EditText mEtCc;
    private EditText mEtLc;
    private TextView tvAddress;
    private TextView tvCurrency;
    private Menu mMenu;

//    public static FragmentPay newInstance(String to, String btcAmount, String lcAmount, boolean isUseAllFundsChecked) {
//        FragmentPay fragment = new FragmentPay();
//        Bundle args = new Bundle();
//        args.putString(ARG_TO, to);
//        args.putString(ARG_BTC, btcAmount);
//        args.putString(ARG_LC, lcAmount);
//        args.putBoolean(ARG_USE_ALL_FUNDS, isUseAllFundsChecked);
//
//        fragment.setArguments(args);
//        return fragment;
//    }

    public void launchQRcodeScanActivity() {
        if (mUseFixAddress) {
            showFixAddressDialog();
        } else {
            Intent intent = new Intent(getActivity(), ActivityQRcodeScan.class);
            startActivityForResult(intent, MainActivity.REQUEST_CODE_QR_CODE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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

    public void pasteAddressFromClipboard() {
        if (mUseFixAddress) {
            showFixAddressDialog();
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
                    showFixAddressDialog();
                } else {
                    DialogReadOtk dialogReadOtk = new DialogReadOtk();
                    dialogReadOtk.show(getFragmentManager(), "ReadOtk");
                }
            }
        });

        mEtCc = view.findViewById(R.id.input_crypto_currency);
        mEtCc.setSelectAllOnFocus(true);
        mEtLc = view.findViewById(R.id.input_local_currency);
        mEtLc.setSelectAllOnFocus(true);
        mEtLc.setText("-.-");
        mEtLc.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && objExchangeRate == null) {
                    AlertPrompt.alert(getContext(), "Could not get exchange rate.");
                    mEtLc.clearFocus();
                    mEtCc.requestFocus();
                }
            }
        });
        updateLocalCurrency(Preferences.getLocalCurrency());

        MainActivity.setOnlineDataUpdateListner(new MainActivity.OnlineDataUpdateListener() {
            @Override
            public void onExchangeRateUpdated(ExchangeRate exchangeRate) {
                mEtLc.setText("");
                objExchangeRate = exchangeRate;
                updateCurrency();
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
                }
                else {
                    showFixAddressDialog();
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

        if (getArguments() != null) {
            mEtCc.setText(getArguments().getString(ARG_BTC));
            try {
                mBtc = Double.parseDouble(mEtCc.getText().toString());
            } catch (NumberFormatException e) {
                mEtCc.setText("");
            }
            mEtLc.setText(getArguments().getString(ARG_LC));
            String to = getArguments().getString(ARG_TO);
            if (to != null && to.length() > 0) {
                tvAddress.setText(to);
            }
            CheckBox cb = view.findViewById(R.id.checkBox_use_all_funds);
            boolean b = getArguments().getBoolean(ARG_USE_ALL_FUNDS);
            logger.debug("Use all funds checked:" + b);
            cb.setChecked(b);
            updateCurrency();
        }

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
                    mEtLc.setText(String.format(Locale.ENGLISH, "%.2f", toLocalCurrency(cc)));
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
                    mBtc = getCryptoCurrency(lc);
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
                    CheckBox cb = getView().findViewById(R.id.checkBox_use_all_funds);
                    if (cb != null && !cb.isChecked() && mBtc <= 0) {
                        makeToastMessage(getString(R.string.incorrect_amount));
                        return;
                    }

                    DialogReadOtk dialogReadOtk = new DialogReadOtk();
                    dialogReadOtk.show(getFragmentManager(), "ReadOtk");

//                    if (mListener != null) {
//                        if (cb != null) {
//                            mListener.onSignPaymentButtonClick(tvAddress.getText().toString(), mBtc, mEtCc.getText().toString(), mEtLc.getText().toString(), cb.isChecked());
//                        }
//                    }
                } catch (NullPointerException | NumberFormatException e) {
                    e.printStackTrace();
                    makeToastMessage(getString(R.string.incorrect_recipient_amount));
                }
            }
        });

        CheckBox cb = view.findViewById(R.id.checkBox_use_all_funds);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mEtCc.setEnabled(false);
                    mEtLc.setEnabled(false);
                } else {
                    mEtCc.setEnabled(true);
                    mEtLc.setEnabled(true);
                }
            }
        });

        // Use All Fund might be checked already, update amount field if it's editable
        if (cb.isChecked()) {
            mEtCc.setEnabled(false);
            mEtLc.setEnabled(false);
        } else {
            mEtCc.setEnabled(true);
            mEtLc.setEnabled(true);
        }

        return view;
    }

    @Override
    public void onPageSelected() {
        super.onPageSelected();

        if (Preferences.getUseFixAddressChecked()) {
            if (MainActivity.getPayToAddress().length() > 0) {
                showFixAddressDialog();
            }
            tvAddress.setText(Preferences.getUseFixAddressAddrString());
            mUseFixAddress = true;
        }
        else {
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

    private double toLocalCurrency(double amount) {
        if (amount > 0 && objExchangeRate != null) {
            switch (mLocalCurrency) {
                case LOCAL_CURRENCY_TWD:
                    return (objExchangeRate.getTWD() * amount);
                case LOCAL_CURRENCY_USD:
                    return (objExchangeRate.getUSD() * amount);
                case LOCAL_CURRENCY_CNY:
                    return (objExchangeRate.getCNY() * amount);
                case LOCAL_CURRENCY_EUR:
                    return (objExchangeRate.getEUR() * amount);
                case LOCAL_CURRENCY_JPY:
                    return (objExchangeRate.getJPY() * amount);
            }
        }

        return 0;
    }

    public void updateAmount(String amount) {
        View view = getView();
        TextView tv = null;
        if (view != null) {
            tv = view.findViewById(R.id.input_crypto_currency);
        }
        if (tv != null) {
            tv.setText(amount);
        }
    }

    private double getCryptoCurrency(double lc) {
        if (objExchangeRate == null) {
            return 0;
        }
        switch (mLocalCurrency) {
            case LOCAL_CURRENCY_TWD:
                return lc / objExchangeRate.getTWD();
            case LOCAL_CURRENCY_USD:
                return lc / objExchangeRate.getUSD();
            case LOCAL_CURRENCY_CNY:
                return lc / objExchangeRate.getCNY();
            case LOCAL_CURRENCY_EUR:
                return lc / objExchangeRate.getEUR();
            case LOCAL_CURRENCY_JPY:
                return lc / objExchangeRate.getJPY();
        }
        return 0;
    }

    private void updateCurrency() {
        try {
            if (mIsCryptoCurrencySet) {
                if (mBtc == 0.0) {
                    // No need to update if amount is 0.
                    return;
                }
                // update local currency
                mIsAmountConverting = true;
                try {
                    mEtLc.setText(String.format(Locale.ENGLISH, "%.2f", toLocalCurrency(mBtc)));
                } catch (NumberFormatException e) {
                    mEtLc.setText("");
                }
                mIsAmountConverting = false;
            } else {
                mIsAmountConverting = true;
                try {
                    double lc = Double.parseDouble(mEtLc.getText().toString());
                    mBtc = getCryptoCurrency(lc);
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

    public void updateLocalCurrency(LocalCurrency localCurrency) {
        mLocalCurrency = localCurrency;
        tvCurrency.setText(mLocalCurrency.toString());
        boolean cache = mIsCryptoCurrencySet;
        mIsCryptoCurrencySet = true;
        updateCurrency();
        mIsCryptoCurrencySet = cache;
    }

    public LocalCurrency currencyToLocalCurrency(int currency) {
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

    Configurations.TxFeeType toTxFee(int transactionFee) {
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

    public void dialogLocalCurrency() {
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
                LocalCurrency lc = currencyToLocalCurrency(localCurrency);
                Preferences.setLocalCurrency(lc);
                updateLocalCurrency(lc);
                updatePayConfig(mMenu);
            }
        });
        dialog.show(getFragmentManager(), "dialog");
    }

    public void dialogTransactionFee() {
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
        dialog.show(getFragmentManager(), "dialog");
    }

    public void dialogAbout() {
        DialogAbout dialog = new DialogAbout();
        dialog.show(getFragmentManager(), "dialog");
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
                mUseFixAddress = processFixAddressClick(!item.isChecked());
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

    public interface FragmentPayListener {
//        void ontvAddress.getText().toStringByReadNfcButtonClick(String to, String btc, String lc, boolean isAllFundChecked);

        void onSignPaymentButtonClick(String to, double amount, String btc, String lc, boolean isAllFundChecked);
    }

    public boolean processFixAddressClick(boolean isChecked) {
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

    private void showFixAddressDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.cannot_change_addr)
                .setMessage(R.string.disable_fix_addr_first)
                .setNegativeButton(R.string.ok, null)
                .show();
        MainActivity.setPayToAddress("");
    }

    @Override
    public void onOtkDataPosted(OtkData otkData) {
        super.onOtkDataPosted(otkData);

        // process received otk data
        if (otkData != null) {
            if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_NA) {
                if (setPayer && addrPayer == null) {
                    // check balance from payer address
                    // construct transaction
                    // show confirmation dialog
                }
                else {
                    // read OTK general information, must be getting the OTK address
                    String addr = otkData.getSessionData().getAddress();

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
                    }
                    else {
                        AlertPrompt.alert(getContext(), "Pay fail\n"
                                + getString(R.string.reason) + ": " + otkData.getFailureReason());
                    }
                }
                else {
                    AlertPrompt.alert(getContext(), getString(R.string.communication_error)+ "\n"
                            + getString(R.string.reason) + ": " + getString(R.string.not_valid_openturnkey));
                }
            }
        }
    }

    public String transactionFeeToString(Configurations.TxFeeType txFeeType) {
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

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    public void updatePayConfig(Menu menu) {
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
                    " (" + transactionFeeToString(txFeeType) + ")";
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_pay, menu);

        updatePayConfig(menu);
        mMenu = menu;
    }

    private void makeToastMessage(String message) {
        Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
        TextView textView = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextSize(22);
        snackbar.setAction("Action", null).show();
    }
}
