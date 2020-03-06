package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.db.OpenturnkeyDB;


public class DialogClearHistory extends AppCompatDialogFragment {
    public DialogClearHistoryListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = View.inflate(getContext(), R.layout.dialog_clear_history, null);

        builder.setView(view)
                .setTitle(R.string.clear_history)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Clear payment history
                        OpenturnkeyDB otkDB = new OpenturnkeyDB(getContext());
                        if (otkDB.clearTransactionTable()) {
                            Toast.makeText(getContext(), getString(R.string.all_history_cleared), Toast.LENGTH_LONG).show();
                            if (null != mListener) {
                                mListener.onHistoryCleared();
                            }
                        }
                        else {
                            Toast.makeText(getContext(), getString(R.string.failed_to_clear_history), Toast.LENGTH_LONG).show();
                        }
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (mListener != null) return;

        try {
            mListener = (DialogClearHistoryListener) context;
        } catch (Exception ignored) { }

    }

    public void setDialogClearHistoryListener(DialogClearHistoryListener listener) {
        this.mListener = listener;
    }

    public interface DialogClearHistoryListener {
        void onHistoryCleared();
    }
}
