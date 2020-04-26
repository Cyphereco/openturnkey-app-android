package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;

import com.cyphereco.openturnkey.R;

import java.util.Objects;

public class DialogWarningOnRequestConfirmation {
    private AlertDialog.Builder builder;

    DialogWarningOnRequestConfirmation(@NonNull Context context,
                                       String title,
                                       String message,
                                       boolean cancelable) {

        builder = new AlertDialog.Builder(context, R.style.AlertDialogWarningStyle)
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

    void setConfirmedButton(String hint, final OnConfirmedListener listener) {
        builder.setPositiveButton(hint, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onConfirmed();
            }
        });
    }

    public void show() {
        Dialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public interface OnConfirmedListener {
        void onConfirmed();
    }
}
