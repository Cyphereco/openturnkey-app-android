package com.cyphereco.openturnkey.ui;

import android.annotation.SuppressLint;
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

import com.cyphereco.openturnkey.BuildConfig;
import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.webservices.BlockCypher;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Objects;

public class DialogAbout extends AppCompatDialogFragment {
    public static final String TAG = DialogAbout.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);
    int clickCount = 0;
    final static String TESTNET_POSTFIX = "(t)";

    private void setVersion(View v) {
        TextView tvVersion = v.findViewById(R.id.version_number);
        String ver = BuildConfig.VERSION_NAME;
        tvVersion.setText(ver);
        if (Preferences.isTestnet()) {
            // Add postfix 't' to version number
            tvVersion.setText(String.format("%s%s", tvVersion.getText(), TESTNET_POSTFIX));
        }
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_about, null);

        builder.setView(view)
                .setTitle(R.string.about)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        final TextView tvVersion = view.findViewById(R.id.version_number);
        tvVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logger.debug("onClick:{}", ++clickCount);
                if (clickCount >= 3) {
                    // toggle mode
                    if (Preferences.isTestnet()) {
                        Preferences.setNetwork(Configurations.Network.MAINNET);
                        BlockCypher.reInit();
                        setVersion(v);
                    } else {
                        Preferences.setNetwork(Configurations.Network.TESTNET);
                        BlockCypher.reInit();
                        setVersion(v);
                    }
                    clickCount = 0;
                }
            }
        });

        setVersion(view);
        Dialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_read_otk_round);
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;
//        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }
}
