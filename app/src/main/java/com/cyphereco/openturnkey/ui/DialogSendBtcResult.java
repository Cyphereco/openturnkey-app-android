package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public class DialogSendBtcResult extends AppCompatDialogFragment {
    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        int resultStringId = R.string.transaction_receipt;
        String reason = "";
        String from;
        String to;
        String hash;
        double amount;
        double fee;
        String time;
        String raw;

        if (getArguments() != null) {
            resultStringId = getArguments().getInt("sendBtcResult");
            reason = getArguments().getString("failureReason");
        }

        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        View view;
        if (resultStringId == R.string.transaction_receipt) {
            hash = getArguments().getString("hash");
            from = getArguments().getString("from");
            to = getArguments().getString("to");
            amount = getArguments().getDouble("amount");
            fee = getArguments().getDouble("fee");
            time = getArguments().getString("time");
            raw = getArguments().getString("raw");
            view = inflater.inflate(R.layout.dialog_btc_sent, null);
            TextView tv;
            tv = view.findViewById(R.id.from);
            tv.setText(from);
            tv = view.findViewById(R.id.to);
            tv.setText(to);
            tv = view.findViewById(R.id.amount);
            tv.setText(String.format(Locale.getDefault(), "%.8f", amount));
            tv = view.findViewById(R.id.fee);
            tv.setText(String.format(Locale.getDefault(), "%.8f", fee));
            tv = view.findViewById(R.id.hash);
            tv.setText(hash);
            tv = view.findViewById(R.id.time);
            tv.setText(time);
        }
        else {
            view = inflater.inflate(R.layout.dialog_send_btc_failed, null);
            TextView tv = view.findViewById(R.id.failure_reason);
            tv.setText(reason);
        }

        builder.setView(view)
                .setTitle(resultStringId)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        Dialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }
}
