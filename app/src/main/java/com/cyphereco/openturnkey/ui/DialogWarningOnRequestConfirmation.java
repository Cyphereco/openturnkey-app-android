package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cyphereco.openturnkey.R;

import java.util.Objects;

public class DialogWarningOnRequestConfirmation {
    private AlertDialog.Builder builder;
    private Dialog dialog;
    private Context dialogContext;
    private String dialogTitle;
    private String dialogMessage;
    private boolean dialogCancelable;

    DialogWarningOnRequestConfirmation(@NonNull Context context,
                                       String title,
                                       String message,
                                       boolean cancelable) {
        dialogContext = context;
        dialogTitle = title;
        dialogMessage = message;
        dialogCancelable = cancelable;

        builder = new AlertDialog.Builder(dialogContext, R.style.AlertDialogWarningStyle)
                .setTitle(dialogTitle)
                .setMessage(dialogMessage);

        if (dialogCancelable) {
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
