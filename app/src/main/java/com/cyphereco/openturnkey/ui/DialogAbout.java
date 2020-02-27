package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.cyphereco.openturnkey.BuildConfig;


import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Configurations;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import com.cyphereco.openturnkey.webservices.BlockCypher;

import org.slf4j.Logger;

public class DialogAbout extends AppCompatDialogFragment {
    public static final String TAG = DialogAbout.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);
    int clickCount = 0;
    final static String TESTNET_POSTFIX = "(t)";

    private void setVersion(View v) {
        TextView tvVersion = v.findViewById(R.id.version_number);
        String ver = BuildConfig.VERSION_NAME;
        tvVersion.setText(ver);
        if (Preferences.isTestnet(getContext())) {
            // Add postfix 't' to version number
            tvVersion.setText(String.format("%s%s", tvVersion.getText(), TESTNET_POSTFIX));
        }
        else {
            // Remove (t)
            if (ver.substring((ver.length() - TESTNET_POSTFIX.length()), ver.length()).equals(TESTNET_POSTFIX)) {
                tvVersion.setText(ver.substring(0, ver.length() - TESTNET_POSTFIX.length()));
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_about, null);

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
                    if (Preferences.isTestnet(getContext())) {
                        Preferences.setNetwork(getContext(), Configurations.Network.MAINNET);
                        BlockCypher.getInstance(getContext()).reInit();
                        setVersion(v);
                    }
                    else {
                        Preferences.setNetwork(getContext(), Configurations.Network.TESTNET);
                        BlockCypher.getInstance(getContext()).reInit();
                        setVersion(v);
                    }
                    clickCount = 0;
                }
            }
        });

        setVersion(view);
        return builder.create();
    }
}
