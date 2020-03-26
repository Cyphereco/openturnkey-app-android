package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cyphereco.openturnkey.R;

public class DialogConfirmationRequest {
    private AlertDialog.Builder builder;
    private Dialog dialog;

    DialogConfirmationRequest(@NonNull Context context,
                              String title,
                              String message,
                              boolean cancelable) {
        builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message);

        if (cancelable) {
            builder.setNegativeButton(R.string.cancel, null)
                    .setCancelable(true);
        }
    }

    public void setTitle(String title) {
        builder.setTitle(title);
    }

    public void setMessage(String message) {
        builder.setMessage(message);
    }

    public void setConfirmedButton(String hint, final OnConfirmedListener listener) {
        builder.setPositiveButton(hint, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onConfirmed();
            }
        });
    }

    public void show() {
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public interface OnConfirmedListener {
        void onConfirmed();
    }
}
