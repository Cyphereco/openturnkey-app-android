package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Otk;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FragmentOtk extends Fragment {

    public static final String TAG = FragmentOtk.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);
    // Cancel timeout for pay
    private final int AUTO_DISMISS_MILLIS = 10 * 1000;

    private FragmentOtkListener mListener;
    private static final String ARG_OPERATION = "operation";
    private Otk.Operation mOp;
    private CountDownTimer mCancelTimer = null;

    public static FragmentOtk newInstance(Otk.Operation op) {
        FragmentOtk fragment = new FragmentOtk();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OPERATION, op);
        fragment.setArguments(args);
        return fragment;
    }

    public void hideCancelButton() {
        // Stop cancel timer anyway
        stopCancelTimer();
        Button btn = getView().findViewById(R.id.button_otk_cancel);
        if (btn != null) {
            btn.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mOp = (Otk.Operation) getArguments().getSerializable(ARG_OPERATION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_openturnkey, container, false);

        final TextView tv;
        tv = view.findViewById(R.id.text_nfc_comm_type);
        // Cancel button
        final Button btn = view.findViewById(R.id.button_otk_cancel);
        logger.info("mOp:" + mOp.toString());
        if (mOp == Otk.Operation.OTK_OP_SIGN_PAYMENT) {
            tv.setText(R.string.sign_payment);
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopCancelTimer();
                    if (mListener != null) {
                        mListener.onCancelButtonClick();
                    }
                }
            });
            mCancelTimer = new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    btn.setText(String.format(
                            Locale.getDefault(), "%s (%d)",
                            getString(R.string.cancel),
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1 //add one so it never displays zero
                    ));
                }
                public void onFinish() {
                    if (mListener != null) {
                        mListener.onCancelTimeout();
                        btn.setVisibility(View.INVISIBLE);
                        // Default is read general info
                        tv.setText(R.string.read_general_information);
                    }
                }
            };
            mCancelTimer.start();
        }
        else if (mOp == Otk.Operation.OTK_OP_GET_RECIPIENT_ADDRESS) {
            tv.setText(R.string.get_recipient_address);
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onCancelButtonClick();
                    }
                }
            });
        }
        else if (mOp == Otk.Operation.OTK_OP_GET_KEY) {
            tv.setText(R.string.read_key_information);
            btn.setVisibility(View.INVISIBLE);
        }
        else if (mOp == Otk.Operation.OTK_OP_SET_PIN_CODE) {
            tv.setText(R.string.set_pin_code);
            btn.setVisibility(View.INVISIBLE);
        }
        else if (mOp == Otk.Operation.OTK_OP_WRITE_MEMO) {
            tv.setText(R.string.write_memo);
            btn.setVisibility(View.INVISIBLE);
        }
        else if (mOp == Otk.Operation.OTK_OP_UNLOCK) {
            tv.setText(R.string.unlock);
            btn.setVisibility(View.INVISIBLE);
        }
        else if (mOp == Otk.Operation.OTK_OP_SIGN_MESSAGE) {
            tv.setText(R.string.sign_message);
            btn.setVisibility(View.INVISIBLE);
        }
        else if (mOp == Otk.Operation.OTK_OP_CHOOSE_KEY) {
            tv.setText(R.string.set_key);
            btn.setVisibility(View.INVISIBLE);
        }
        else {
            btn.setVisibility(View.INVISIBLE);
            // Default is read general info
            tv.setText(R.string.read_general_information);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentOtkListener) {
            mListener = (FragmentOtkListener) context;
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

    public void stopCancelTimer() {
        if (mCancelTimer != null) {
            mCancelTimer.cancel();
        }
        Button btn = getView().findViewById(R.id.button_otk_cancel);
        btn.setText(R.string.cancel);
    }

    public interface FragmentOtkListener {
        void onCancelButtonClick();
        void onCancelTimeout();
    }
}
