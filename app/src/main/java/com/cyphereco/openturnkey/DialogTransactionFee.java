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

public class DialogTransactionFee extends AppCompatDialogFragment {
    private int selectedFee;
    RadioGroup rgTransactionFee = null;
    public DialogTransactionFeeListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_transaction_fee, null);

        builder.setView(view)
                .setTitle(R.string.transaction_fee)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.setTransactionFee(rgTransactionFee.getCheckedRadioButtonId());
                    }
                });

        rgTransactionFee = view.findViewById(R.id.rg_transaction_fee);

        if (getArguments() != null) {
            selectedFee = getArguments().getInt("transactionFee");
            rgTransactionFee.check(selectedFee);
        }

        return builder.create();
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (DialogTransactionFeeListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString() +
                    "must implement DialogTransactionFeeListener");
        }

    }

    public interface DialogTransactionFeeListener {
        void setTransactionFee(int transactionFee);
    }
}
