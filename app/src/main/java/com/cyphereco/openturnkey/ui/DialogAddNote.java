package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;

public class DialogAddNote extends AppCompatDialogFragment {
    public DialogAddNoteListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_add_note, null);

        builder.setView(view)
                .setTitle(R.string.write_memo)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.cancelAddNote();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        TextView tv = view.findViewById(R.id.note);
                        listener.addNote(tv.getText().toString());
                    }
            });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            listener = (DialogAddNoteListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString() +
                    "must implement DialogLocalCurrecyListener");
        }

    }

    public interface DialogAddNoteListener {
        void addNote(String note);
        void cancelAddNote();
    }
}
