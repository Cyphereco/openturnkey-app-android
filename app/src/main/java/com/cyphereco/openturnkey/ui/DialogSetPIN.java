package com.cyphereco.openturnkey.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DialogSetPIN extends AppCompatDialogFragment {
    public DialogSetPINListener listener;

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.dialog_set_pin, null);

        builder.setView(view)
                .setTitle(R.string.title_set_pin)
                .setCancelable(false);

        Button btnOk;
        btnOk = view.findViewById(R.id.button_set_pin_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText pin = view.findViewById(R.id.textInputSetPin);
                TextInputEditText pinConfirm = view.findViewById(R.id.textInputSetPinConfirm);
                if (Objects.requireNonNull(pin.getText()).toString().length() != 8) {
                    Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), getString(R.string.pin_length_incorrect), Toast.LENGTH_LONG).show();
                    return;
                }
                if ((pin.getText().toString().equals(Objects.requireNonNull(pinConfirm.getText()).toString()))) {
                    listener.setPin(pin.getText().toString());
                    dismiss();
                } else {
                    Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), getString(R.string.pin_confirmation_not_match), Toast.LENGTH_LONG).show();
                }
            }
        });
        Button btnCancel;
        btnCancel = view.findViewById(R.id.button_set_pin_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    public void setListener(DialogSetPINListener listener) {
        this.listener = listener;
    }

    public interface DialogSetPINListener {
        void setPin(String pin);
    }
}
