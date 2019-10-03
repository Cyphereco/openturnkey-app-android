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
                        if (OpenturnkeyDB.ReturnValue.SUCCESS == otkDB.clearTransactionTable()) {
                            Toast.makeText(getContext(), "Success to clear history",
                                    Toast.LENGTH_LONG).show();
                            if (null != mListener) {
                                mListener.onClearHistorySuccess();
                            }
                        }
                        else {
                            Toast.makeText(getContext(), "Fail to clear history",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (DialogClearHistoryListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString() +
                    "must implement DialogClearHistoryListener");
        }

    }

    public interface DialogClearHistoryListener {
        void onClearHistorySuccess();
    }
}
