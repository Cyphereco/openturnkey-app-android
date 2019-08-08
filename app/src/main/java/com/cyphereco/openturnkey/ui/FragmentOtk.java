package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.os.Bundle;
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

import static com.cyphereco.openturnkey.core.Otk.Operation.OTK_OP_SIGN_PAYMENT;
import static com.cyphereco.openturnkey.core.Otk.Operation.OTK_OP_GET_RECIPIENT_ADDRESS;


public class FragmentOtk extends Fragment {

    public static final String TAG = FragmentOtk.class.getSimpleName();

    private FragmentOtkListener mListener;
    private static final String ARG_OPERATION = "operation";
    private Otk.Operation mOp;

    public static FragmentOtk newInstance(Otk.Operation op) {
        FragmentOtk fragment = new FragmentOtk();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OPERATION, op);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        if (getArguments() != null) {
            mOp = (Otk.Operation) getArguments().getSerializable(ARG_OPERATION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_openturnkey, container, false);

        TextView tv;
        tv = view.findViewById(R.id.text_nfc_comm_type);
        Button btn = view.findViewById(R.id.button_otk_cancel);
        if (mOp == OTK_OP_SIGN_PAYMENT) {
            tv.setText(R.string.sign_payment);
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onCancelButtonClick();
                    }
                    //getActivity().onBackPressed();
                }
            });
        }
        else if (mOp == OTK_OP_GET_RECIPIENT_ADDRESS) {
            tv.setText(R.string.get_recipient_address);
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onCancelButtonClick();
                    }
                    //getActivity().onBackPressed();
                }
            });
        }
        else {
            btn.setVisibility(View.INVISIBLE);
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

    public interface FragmentOtkListener {
        void onCancelButtonClick();
    }
}
