package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import com.cyphereco.openturnkey.R;

import java.util.Objects;

public class DialogLocalCurrency extends AppCompatDialogFragment {
    private int selectedCurrency;
    RadioGroup rgLocalCurrencies = null;
    public DialogLocalCurrecyListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_local_currency, null);

        builder.setView(view)
                .setTitle(R.string.local_currency)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null)
                            listener.setLocalCurrency(rgLocalCurrencies.getCheckedRadioButtonId());
                    }
                });

        rgLocalCurrencies = view.findViewById(R.id.rg_local_currency);

        if (getArguments() != null) {
            selectedCurrency = getArguments().getInt("localCurrency");
            rgLocalCurrencies.check(selectedCurrency);
        }

        Dialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (listener != null) return;

        try {
            listener = (DialogLocalCurrecyListener) context;
        } catch (Exception ignored) { }

    }

    public void setDialogLocalCurrecyListener(DialogLocalCurrecyListener listener) {
        this.listener = listener;
    }

    public interface DialogLocalCurrecyListener {
        void setLocalCurrency(int localCurrency);
    }
}
