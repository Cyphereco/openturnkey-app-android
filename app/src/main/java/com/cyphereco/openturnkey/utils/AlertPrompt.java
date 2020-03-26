package com.cyphereco.openturnkey.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.cyphereco.openturnkey.R;

public class AlertPrompt {
    private static final int decorInfo = R.drawable.custom_toast_info_round;
    private static final int decorAlert = R.drawable.custom_toast_alert_round;

    public static void info(Context context, String msg) {
        prompt(context, msg, decorInfo, false);
    }

    public static void threadSafeInfo(Context context, String msg) {
        Looper.prepare();
        info(context, msg);
        Looper.loop();
    }

    public static void alert(Context context, String msg) {
        prompt(context, msg, decorAlert, true);
    }

    public static void threadSafeAlert(Context context, String msg) {
        Looper.prepare();
        alert(context, msg);
        Looper.loop();
    }

    private static void prompt(Context context, String msg, int decor, boolean vibration) {
        if (vibration) {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(300);
        }

        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        TextView tv = toast.getView().findViewById(android.R.id.message);
        tv.setTextColor(Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(20);
        tv.setPadding(36, 12, 36, 12);
//        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        toast.getView().setBackgroundResource(decor);
        toast.show();
    }
}
