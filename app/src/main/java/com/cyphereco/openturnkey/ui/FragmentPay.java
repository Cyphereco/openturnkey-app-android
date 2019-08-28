package com.cyphereco.openturnkey.ui;

import android.Manifest;
import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.utils.CurrencyExchangeRate;
import com.cyphereco.openturnkey.utils.LocalCurrency;

public class FragmentPay extends Fragment {
    public static final String TAG = FragmentPay.class.getSimpleName();

    private static final int ZXING_CAMERA_PERMISSION = 1;

    private FragmentPayListener mListener;
    private String mRecipientAddress;

    boolean mIsCryptoCurrencySet = true;
    private LocalCurrency mLocalCurrency = LocalCurrency.LOCAL_CURRENCY_TWD;
    private CurrencyExchangeRate mCurrencyExRate;
    private boolean mIsAmountConverting = false;
    private double mBtc = 0.0;
    private static final String ARG_TO = "TO";
    private static final String ARG_BTC = "BTC";
    private static final String ARG_LC = "LC";


    public static FragmentPay newInstance(String to, String btcAmount, String lcAmount) {
        FragmentPay fragment = new FragmentPay();
        Bundle args = new Bundle();
        args.putString(ARG_TO, to);
        args.putString(ARG_BTC, btcAmount);
        args.putString(ARG_LC, lcAmount);
        fragment.setArguments(args);
        return fragment;
    }

    public void launchQRcodeScanActivity(View v) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent(getContext(), QRcodeScanActivity.class);
            getActivity().startActivityForResult(intent, MainActivity.REQUEST_CODE_QR_CODE);
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
        iv = view.findViewById(R.id.icon_read_nfc);
        final EditText etCc = view.findViewById(R.id.input_crypto_currency);
        final EditText etLc = view.findViewById(R.id.input_local_currency);

        if (getArguments() != null) {
            etCc.setText(getArguments().getString(ARG_BTC));
            try {
                mBtc = Double.parseDouble(etCc.getText().toString());
            }
            catch (NumberFormatException e) {
                etCc.setText("");
            }
            etLc.setText(getArguments().getString(ARG_LC));
            String to = getArguments().getString(ARG_TO);
            if (to != null && to.length() > 0) {
                updateRecipientAddress(view, to);
            }
        }

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onGetRecipientAddressByReadNfcButtonClick(mRecipientAddress, etCc.getText().toString(), etLc.getText().toString());
                }
            }
        });

        etCc.addTextChangedListener(new CurrencyTextWatcher(etCc) {
            @Override
            public void afterTextChanged(Editable editable) {
                if (mIsAmountConverting == true || !etCc.isFocused()) {
                    // It's updated by changing of lc
                    return;
                }
                super.afterTextChanged(editable);
                mIsCryptoCurrencySet = true;
                mIsAmountConverting = true;
                try {
                    double cc = Double.parseDouble(etCc.getText().toString());
                    etLc.setText(String.format("%.2f", Double.valueOf(getLocalCurrency(cc))));
                    mBtc = cc;
                }
                catch (NumberFormatException e) {
                    etLc.setText("");
                }
                mIsAmountConverting = false;
            }
        });

        etLc.addTextChangedListener(new CurrencyTextWatcher(etLc) {
            @Override
            public void afterTextChanged(Editable editable) {
                if (mIsAmountConverting == true || !etLc.isFocused()) {
                    // It's updated by changing of cc or back to this fragment from somewhere
                    return;
                }
                super.afterTextChanged(editable);
                mIsCryptoCurrencySet = false;
                mIsAmountConverting = true;
                try {
                    double lc = Double.parseDouble(etLc.getText().toString());
                    mBtc = Double.valueOf(getCryptoCurrency(lc));
                    etCc.setText(String.format("%.8f", mBtc));

                } catch (NumberFormatException e) {
                    etCc.setText("");
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
                    // TODO check if address is valid
                    if (mRecipientAddress == null || mRecipientAddress.length() == 0) {
                        Snackbar.make(view, getString(R.string.recipient_is_empty), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        return;
                    }
                    // Check amount
                    if (mBtc <= 0) {
                        Snackbar.make(view, getString(R.string.incorrect_amount), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        return;
                    }

                    if (mListener != null) {
                        mListener.onSignPaymentButtonClick(mRecipientAddress, mBtc, etCc.getText().toString(), etLc.getText().toString());
                    }
                }
                catch (NullPointerException | NumberFormatException e) {
                    e.printStackTrace();
                    Snackbar.make(view, getString(R.string.incorrect_recipient_amount), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    return;
                }

            }
        });

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
                return Double.valueOf(mCurrencyExRate.getTWD() * cc);
            case LOCAL_CURRENCY_USD:
                return Double.valueOf(mCurrencyExRate.getUSD() * cc);
            case LOCAL_CURRENCY_CNY:
                return Double.valueOf(mCurrencyExRate.getCNY() * cc);
            case LOCAL_CURRENCY_EUR:
                return Double.valueOf(mCurrencyExRate.getEUR() * cc);
            case LOCAL_CURRENCY_JPY:
                return Double.valueOf(mCurrencyExRate.getJPY() * cc);
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
            EditText etLc = getView().findViewById(R.id.input_local_currency);
            EditText etCc = getView().findViewById(R.id.input_crypto_currency);
            if (mIsCryptoCurrencySet == true) {
                if (mBtc == 0.0) {
                    // No need to update if amount is 0.
                    return;
                }
                // update local currency
                mIsAmountConverting = true;
                try {
                    etLc.setText(String.format("%.2f", Double.valueOf(getLocalCurrency(mBtc))));
                }
                catch (NumberFormatException e) {
                    etLc.setText("");
                }
                mIsAmountConverting = false;
            }
            else {
                mIsAmountConverting = true;
                try {
                    double lc = Double.parseDouble(etLc.getText().toString());
                    mBtc = Double.valueOf(getCryptoCurrency(lc));
                    etCc.setText(String.format("%.8f", mBtc));
                }
                catch (NumberFormatException e) {
                    etCc.setText("");
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
        void onGetRecipientAddressByReadNfcButtonClick(String to, String  btc, String lc);
        void onSignPaymentButtonClick(String to, double amount, String  btc, String lc);
    }
}
