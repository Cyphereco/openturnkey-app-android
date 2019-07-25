package com.cyphereco.openturnkey;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import com.cyphereco.openturnkey.R;

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
                        listener.setLocalCurrency(rgLocalCurrencies.getCheckedRadioButtonId());
                    }
                });

        rgLocalCurrencies = view.findViewById(R.id.rg_local_currency);

        if (getArguments() != null) {
            selectedCurrency = getArguments().getInt("localCurrency");
            rgLocalCurrencies.check(selectedCurrency);
        }

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (DialogLocalCurrecyListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString() +
                    "must implement DialogLocalCurrecyListener");
        }

    }

    public interface DialogLocalCurrecyListener {
        void setLocalCurrency(int localCurrency);
    }
}
