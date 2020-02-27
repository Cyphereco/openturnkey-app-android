package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.cyphereco.openturnkey.utils.BtcUtils;
import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;
import com.cyphereco.openturnkey.utils.LocalCurrency;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.util.Locale;

public class FragmentPay extends Fragment {
    public static final String TAG = FragmentPay.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);

    private static final int ZXING_CAMERA_PERMISSION = 1;

    private FragmentPayListener mListener;
    private String mRecipientAddress;

    boolean mUseFixAddress = false;
    boolean mIsCryptoCurrencySet = true;
    private LocalCurrency mLocalCurrency = LocalCurrency.LOCAL_CURRENCY_USD;
    private CurrencyExchangeRate mCurrencyExRate;
    private boolean mIsAmountConverting = false;
    private double mBtc = 0.0;
    private static final String ARG_TO = "TO";
    private static final String ARG_BTC = "BTC";
    private static final String ARG_LC = "LC";
    private static final String ARG_USE_ALL_FUNDS = "USE_ALL_FUNDS";

    private EditText mEtCc;
    private EditText mEtLc;


    public static FragmentPay newInstance(String to, String btcAmount, String lcAmount, boolean isUseAllFundsChecked) {
        FragmentPay fragment = new FragmentPay();
        Bundle args = new Bundle();
        args.putString(ARG_TO, to);
        args.putString(ARG_BTC, btcAmount);
        args.putString(ARG_LC, lcAmount);
        args.putBoolean(ARG_USE_ALL_FUNDS, isUseAllFundsChecked);

        fragment.setArguments(args);
        return fragment;
    }

    public void launchQRcodeScanActivity(View v) {
        if (mUseFixAddress) {
            showFixAddressDialog();
        }
        else {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
            } else {
                Intent intent = new Intent(getContext(), QRcodeScanActivity.class);
                getActivity().startActivityForResult(intent, MainActivity.REQUEST_CODE_QR_CODE);
            }
        }
    }

    public void pasteAddressFromClipboard(View v) {
        if (mUseFixAddress) {
            showFixAddressDialog();
        }
        else {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip()) {
                android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
                android.content.ClipData data = clipboard.getPrimaryClip();
                if (data != null && description != null) {
                    String address = String.valueOf(data.getItemAt(0).coerceToText(getContext()));
                    updateRecipientAddress(address);
                }
            }
        }
    }

    private void updateCurrencyName(View view) {
        TextView tv = view.findViewById(R.id.text_local_currency);
        tv.setText(mLocalCurrency.toString());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay, container, false);
        // Set not focusable for recipient address so that the hint is shown correctly
        TextInputEditText inputReceipientAddress = view.findViewById(R.id.input_recipient_address);
        inputReceipientAddress.setFocusable(false);

        mLocalCurrency = Preferences.getLocalCurrency(getActivity().getApplicationContext());

        updateCurrencyName(view);
        updateCurrency();

        ImageView iv;
        iv = view.findViewById(R.id.icon_scan_qrcode);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchQRcodeScanActivity(view);
            }
        });

        iv = view.findViewById(R.id.icon_paste_clipboard);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pasteAddressFromClipboard(view);
            }
        });

        iv = view.findViewById(R.id.icon_read_nfc);
        mEtCc = view.findViewById(R.id.input_crypto_currency);
        mEtLc = view.findViewById(R.id.input_local_currency);

        if (getArguments() != null) {
            mEtCc.setText(getArguments().getString(ARG_BTC));
            try {
                mBtc = Double.parseDouble(mEtCc.getText().toString());
            }
            catch (NumberFormatException e) {
                mEtCc.setText("");
            }
            mEtLc.setText(getArguments().getString(ARG_LC));
            String to = getArguments().getString(ARG_TO);
            if (to != null && to.length() > 0) {
                updateRecipientAddress(view, to);
            }
            CheckBox cb = view.findViewById(R.id.checkBox_use_all_funds);
            boolean b = getArguments().getBoolean(ARG_USE_ALL_FUNDS);
            logger.debug("Use all funds checked:" + b);
            cb.setChecked(b);
            updateCurrency();
        }

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUseFixAddress) {
                    showFixAddressDialog();
                }
                else {
                    if (mListener != null) {
                        CheckBox cb = getView().findViewById(R.id.checkBox_use_all_funds);
                        mListener.onGetRecipientAddressByReadNfcButtonClick(mRecipientAddress, mEtCc.getText().toString(), mEtLc.getText().toString(), cb.isChecked());
                    }
                }
            }
        });

        mEtCc.addTextChangedListener(new CurrencyTextWatcher(mEtCc) {
            @Override
            public void afterTextChanged(Editable editable) {
                if (mIsAmountConverting == true || !mEtCc.isFocused()) {
                    // It's updated by changing of lc
                    return;
                }
                super.afterTextChanged(editable);
                mIsCryptoCurrencySet = true;
                mIsAmountConverting = true;
                try {
                    double cc = Double.parseDouble(mEtCc.getText().toString());
                    mEtLc.setText(String.format(Locale.ENGLISH, "%.2f", getLocalCurrency(cc)));
                    mBtc = cc;
                }
                catch (NumberFormatException e) {
                    mEtLc.setText("");
                }
                mIsAmountConverting = false;
            }
        });

        mEtLc.addTextChangedListener(new CurrencyTextWatcher(mEtLc) {
            @Override
            public void afterTextChanged(Editable editable) {
                if (mIsAmountConverting == true || !mEtLc.isFocused()) {
                    // It's updated by changing of cc or back to this fragment from somewhere
                    return;
                }
                super.afterTextChanged(editable);
                mIsCryptoCurrencySet = false;
                mIsAmountConverting = true;
                try {
                    double lc = Double.parseDouble(mEtLc.getText().toString());
                    mBtc = Double.valueOf(getCryptoCurrency(lc));
                    mEtCc.setText(String.format("%.8f", mBtc));

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
                View v = getView();
                try {
                    // Check if address is valid
                    if (mRecipientAddress == null || mRecipientAddress.length() == 0) {
                        Snackbar.make(view, getString(R.string.recipient_is_empty), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        return;
                    }
                    if (true == BtcUtils.isSegWitAddress(!Preferences.isTestnet(getContext()), mRecipientAddress)) {
                        Snackbar.make(view, getString(R.string.seg_wit_address_is_not_supported), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        return;
                    }
                    if (false == BtcUtils.validateAddress(!Preferences.isTestnet(getContext()), mRecipientAddress)) {
                        Snackbar.make(view, getString(R.string.invalid_address), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        return;
                    }
                    CheckBox cb = v.findViewById(R.id.checkBox_use_all_funds);
                    if (!cb.isChecked() && mBtc <= 0) {
                        Snackbar.make(view, getString(R.string.incorrect_amount), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        return;
                    }

                    if (mListener != null) {
                        mListener.onSignPaymentButtonClick(mRecipientAddress, mBtc, mEtCc.getText().toString(), mEtLc.getText().toString(), cb.isChecked());
                    }
                }
                catch (NullPointerException | NumberFormatException e) {
                    e.printStackTrace();
                    Snackbar.make(view, getString(R.string.incorrect_recipient_amount), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    return;
                }

            }
        });

        CheckBox cb = view.findViewById(R.id.checkBox_use_all_funds);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (isChecked) {
                    mEtCc.setEnabled(false);
                    mEtLc.setEnabled(false);
                }
                else {
                    mEtCc.setEnabled(true);
                    mEtLc.setEnabled(true);
                }
            }
        });

        // Use All Fund might be checked already, update amount field if it's editable
        if (cb.isChecked()) {
            mEtCc.setEnabled(false);
            mEtLc.setEnabled(false);
        }
        else {
            mEtCc.setEnabled(true);
            mEtLc.setEnabled(true);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentPayListener) {
            mListener = (FragmentPayListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private double getLocalCurrency(double cc) {
        if (mCurrencyExRate == null) {
            return 0;
        }
        switch (mLocalCurrency) {
            case LOCAL_CURRENCY_TWD:
                return (mCurrencyExRate.getTWD() * cc);
            case LOCAL_CURRENCY_USD:
                return (mCurrencyExRate.getUSD() * cc);
            case LOCAL_CURRENCY_CNY:
                return (mCurrencyExRate.getCNY() * cc);
            case LOCAL_CURRENCY_EUR:
                return (mCurrencyExRate.getEUR() * cc);
            case LOCAL_CURRENCY_JPY:
                return (mCurrencyExRate.getJPY() * cc);
        }
        return 0;
    }

    public void updateRecipientAddress(String recipientAddress) {
        updateRecipientAddress(getView(), recipientAddress);
    }

    public void updateRecipientAddress(View view, String recipientAddress) {
        mRecipientAddress = recipientAddress;
        if (view == null) {
            return;
        }
        TextView tv = view.findViewById(R.id.input_recipient_address);
        tv.setText(recipientAddress);
    }

    public void updateAmount(String amount) {
        View view = getView();
        TextView tv = view.findViewById(R.id.input_crypto_currency);
        tv.setText(amount);
    }

    private double getCryptoCurrency(double lc) {
        if (mCurrencyExRate == null) {
            return 0;
        }
        switch (mLocalCurrency) {
            case LOCAL_CURRENCY_TWD:
                return Double.valueOf(lc / mCurrencyExRate.getTWD());
            case LOCAL_CURRENCY_USD:
                return Double.valueOf(lc / mCurrencyExRate.getUSD());
            case LOCAL_CURRENCY_CNY:
                return Double.valueOf(lc / mCurrencyExRate.getCNY());
            case LOCAL_CURRENCY_EUR:
                return Double.valueOf(lc / mCurrencyExRate.getEUR());
            case LOCAL_CURRENCY_JPY:
                return Double.valueOf(lc / mCurrencyExRate.getJPY());
        }
        return 0;
    }

    private void updateCurrency() {
        try {
            if (mIsCryptoCurrencySet == true) {
                if (mBtc == 0.0) {
                    // No need to update if amount is 0.
                    return;
                }
                // update local currency
                mIsAmountConverting = true;
                try {
                    mEtLc.setText(String.format(Locale.ENGLISH, "%.2f", getLocalCurrency(mBtc)));
                }
                catch (NumberFormatException e) {
                    mEtLc.setText("");
                }
                mIsAmountConverting = false;
            }
            else {
                mIsAmountConverting = true;
                try {
                    double lc = Double.parseDouble(mEtLc.getText().toString());
                    mBtc = Double.valueOf(getCryptoCurrency(lc));
                    mEtCc.setText(String.format("%.8f", mBtc));
                }
                catch (NumberFormatException e) {
                    mEtCc.setText("");
                }
                mIsAmountConverting = false;
            }
        }
        catch (NumberFormatException e) {
            // do nothing e.printStackTrace();
        }
        catch (NullPointerException e) {
            // Do nothing
        }
    }

    public void updateCurrencyExchangeRate(CurrencyExchangeRate rate) {
        // Update currency only if there is no rate set before
        if (mCurrencyExRate == null) {
            mCurrencyExRate = rate;
            updateCurrency();
        }
        else {
            mCurrencyExRate = rate;
        }
    }

    public void updateLocalCurrency(LocalCurrency localCurrency) {
        mLocalCurrency = localCurrency;
        updateCurrencyName(getView());
        boolean cache = mIsCryptoCurrencySet;
        mIsCryptoCurrencySet = true;
        updateCurrency();
        mIsCryptoCurrencySet = cache;
    }

    public interface FragmentPayListener {
        void onGetRecipientAddressByReadNfcButtonClick(String to, String  btc, String lc, boolean isAllFundChecked);
        void onSignPaymentButtonClick(String to, double amount, String  btc, String lc, boolean isAllFundChecked);
    }

    public boolean processFixAddressClick(boolean isChecked) {
        if (isChecked) {
            // If address editor is empty, prompt warning dialog
            if ((null == mRecipientAddress) || (mRecipientAddress.isEmpty())) {
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
            mUseFixAddress = true;
            return true;
        }
        mUseFixAddress = false;
        return false;
    }

    public String getRecipientAddress() {
        return mRecipientAddress;
    }

    public void updateUseFixAddress(boolean isChecked) {
        mUseFixAddress = isChecked;
    }

    private void showFixAddressDialog() {
        new AlertDialog.Builder(getActivity())
                .setMessage("Please disable using fix address first")
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
}
