package com.cyphereco.openturnkey.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;

public class AlertPrompt {
    private static final int decorInfo = R.drawable.custom_toast_info_round;
    private static final int decorAlert = R.drawable.custom_toast_alert_round;

    public static void info(Context context, String msg) {
        prompt(context, msg, decorInfo, false);
    }

    public static void alert(Context context, String msg) {
        prompt(context, msg, decorAlert, true);
    }

    private static void prompt(Context context, String msg, int decor, boolean vibration) {
        if (vibration) {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(300);
        }

        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        TextView tv = toast.getView().findViewById(android.R.id.message);
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(22);
        tv.setMinWidth(200);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        toast.getView().setBackgroundResource(decor);
        toast.show();
    }
}