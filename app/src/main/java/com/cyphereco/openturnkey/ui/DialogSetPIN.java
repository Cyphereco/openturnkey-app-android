package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;

public class DialogSetPIN extends AppCompatDialogFragment {
    public DialogSetPINListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_set_pin, null);

        builder.setView(view)
                .setTitle(R.string.title_set_pin)
                .setCancelable(false);

        Button btnOk;
        btnOk = view.findViewById(R.id.button_set_pin_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextInputEditText pin = view.findViewById(R.id.textInputSetPin);
                TextInputEditText pinConfirm = view.findViewById(R.id.textInputSetPinConfirm);;
                if (pin.getText().toString().length() != 8) {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.pin_length_incorrect), Toast.LENGTH_LONG).show();
                    return;
                }
                if ((pin.getText().toString().equals(pinConfirm.getText().toString()))) {
                    listener.setPIN(pin.getText().toString());
                    dismiss();
                }
                else {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.pin_confirmation_not_match), Toast.LENGTH_LONG).show();
                }
            }
        });
        Button btnCancel;
        btnCancel = view.findViewById(R.id.button_set_pin_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.cancelSetPIN();
                dismiss();
            }
        });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (DialogSetPINListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString() +
                    "must implement DialogSetPINListener");
        }

    }

    public interface DialogSetPINListener {
        void setPIN(String note);
        void cancelSetPIN();
    }
}
