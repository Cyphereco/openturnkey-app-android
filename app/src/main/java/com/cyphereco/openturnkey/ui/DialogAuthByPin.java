package com.cyphereco.openturnkey.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.poovam.pinedittextfield.CirclePinField;

import com.cyphereco.openturnkey.R;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DialogAuthByPin extends AppCompatDialogFragment {

    public DialogAuthByPinListener listener;

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_auth_with_pin, null);

        builder.setView(view)
                .setTitle(R.string.auth_with_pin)
                .setCancelable(false);

        CirclePinField pinField = view.findViewById(R.id.pre_auth_pin_field);
        pinField.requestFocus();

        InputMethodManager imm = (InputMethodManager) Objects.requireNonNull(getContext()).getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        pinField.setOnTextCompleteListener(new CirclePinField.OnTextCompleteListener() {
            @Override
            public boolean onTextComplete(@NotNull String enteredText) {
                listener.setPin(enteredText);
                DialogAuthByPin.this.dismiss();
                return false; // Return false to keep the keyboard open else return true to close the keyboard
            }
        });

        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void dismiss() {
        InputMethodManager imm = (InputMethodManager) Objects.requireNonNull(getContext()).getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        super.dismiss();
    }

    public void setListener(DialogAuthByPinListener listener) {
        this.listener = listener;
    }

    public interface DialogAuthByPinListener {
        void setPin(String pin);
    }
}
