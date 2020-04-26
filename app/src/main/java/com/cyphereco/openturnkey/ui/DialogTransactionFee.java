package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.utils.BtcUtils;

import java.text.DecimalFormat;
import java.util.Objects;

public class DialogTransactionFee extends AppCompatDialogFragment {
    private int selectedFee;
    RadioGroup rgTransactionFee = null;
    EditText et = null;
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
                        if (listener == null) return;;
                        View v = getView();
                        int selected = rgTransactionFee.getCheckedRadioButtonId();
                        try {
                            double txFee = Double.valueOf(et.getText().toString());
                            listener.setCustomizedTxFee(txFee);
                        }
                        catch (Exception e) {
                            // Should not be here
                            e.printStackTrace();
                        }
                        listener.setTransactionFee(rgTransactionFee.getCheckedRadioButtonId());

                    }
                });

        rgTransactionFee = view.findViewById(R.id.rg_transaction_fee);
        et = view.findViewById(R.id.transaction_fee);

        if (getArguments() != null) {
            selectedFee = getArguments().getInt("transactionFee");
            rgTransactionFee.check(selectedFee);
            double fee = BtcUtils.satoshiToBtc(getArguments().getLong("customizedFee"));
            if (fee >= 0) {
                EditText et = view.findViewById(R.id.transaction_fee);
                DecimalFormat format = new DecimalFormat("0.########");
                et.setText(format.format(fee));
            }
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
            listener = (DialogTransactionFeeListener) context;
        } catch (Exception ignored) { }
    }

    public void setDialogTransactionFeeListener(DialogTransactionFeeListener listener) {
        this.listener = listener;
    }

    public interface DialogTransactionFeeListener {
        void setTransactionFee(int transactionFee);
        void setCustomizedTxFee(double txFee);
    }
}
