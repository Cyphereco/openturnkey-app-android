package com.cyphereco.openturnkey.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.poovam.pinedittextfield.CirclePinField;

import com.cyphereco.openturnkey.R;

public class DialogAuthByPin extends AppCompatDialogFragment {
    private String mPin = "";

    public DialogAuthByPinListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_auth_with_pin, null);

        builder.setView(view)
                .setTitle(R.string.pre_auth_by_pin)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.cancelAuthByPin();
                    }
                });

        CirclePinField pinField = view.findViewById(R.id.pre_auth_pin_field);
        pinField.requestFocus();

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        pinField.setOnTextCompleteListener(new CirclePinField.OnTextCompleteListener() {
            @Override
            public boolean onTextComplete (String enteredText) {
                mPin = enteredText;
                listener.authByPin(mPin);
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                DialogAuthByPin.this.dismiss();
                return false; // Return false to keep the keyboard open else return true to close the keyboard
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (DialogAuthByPinListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString() +
                    "must implement DialogLocalCurrecyListener");
        }

    }

    public interface DialogAuthByPinListener {
        void authByPin(String pin);
        void cancelAuthByPin();
    }
}
