package com.cyphereco.openturnkey;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cyphereco.openturnkey.R;

public class FragmentPay extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay, container, false);
        // Set not focusable for recipient address so that the hint is shown correctly
        TextInputEditText inputReceipientAddress = view.findViewById(R.id.input_recipient_address);
        inputReceipientAddress.setFocusable(false);
        return view;
    }
}
