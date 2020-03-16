package com.cyphereco.openturnkey.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;

public class DialogReadOtk extends AppCompatDialogFragment {
    public static final int DIALOG_LIFETIME = 15000;
    public static final int SHOW_RESULT_DELAY = 2000;
    public static final int DISMISS_ANIMATION_TIME = 250;

    private static Button cancelButton;
    private static TextView textTitle;
    private static TextView textDesc;
    private static ImageView iconHint;
    private static ImageView iconSuccess;
    private static ImageView iconFail;

    private static dialogReadOtkListener dialogListner;
    private static CountDownTimer dialogTImer;

    final public static int NOT_OPENTURNKEY   = 0;
    final public static int READ_SUCCESS      = 1;
    final public static int REQUEST_FAIL      = 2;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_read_otk, null);

        final Dialog dialog = new AlertDialog.Builder(getActivity()).setView(view).create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;
        dialog.setCanceledOnTouchOutside(false);

        textTitle = view.findViewById(R.id.text_read_otk_title);
        textDesc = view.findViewById(R.id.text_read_otk_desc);
        iconHint = view.findViewById(R.id.image_read_otk_hint);
        iconSuccess = view.findViewById(R.id.image_read_otk_success);
        iconFail = view.findViewById(R.id.image_read_otk_fail);
        cancelButton = view.findViewById(R.id.btn_cancel_read_otk);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogTImer.cancel();
                // turn off the nfc reading for otk
                MainActivity.disableReadOtk();
                if (dialogListner != null) dialogListner.onCancel();
                dismissAnimation(getDialog().getWindow().getDecorView());
            }
        });

        setCancelTimer();

        return dialog;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity.enableReadOtk();
    }

    private void dismissAnimation(View view) {
        ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat("translationY", 0, 1200f),
                PropertyValuesHolder.ofFloat("alpha", 1.0f, 0.0f));

        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dismiss();
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
            @Override
            public void onAnimationCancel(Animator animation) {
            }
            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animation.setDuration(DISMISS_ANIMATION_TIME);
        animation.start();
    }

    public static void updateReadOtkDesc(String desc) {
        textDesc.setText(desc);
    }

    public static void updateReadOtkStatus(int flag) {
        textTitle.setVisibility(View.INVISIBLE);
        cancelButton.setVisibility(View.INVISIBLE);
        iconHint.setVisibility(View.INVISIBLE);

        switch (flag) {
            case READ_SUCCESS:
                textDesc.setText(R.string.request_sent);
                iconSuccess.setVisibility(View.VISIBLE);
                break;
            case REQUEST_FAIL:
                textDesc.setText(R.string.request_fail);
                iconFail.setVisibility(View.VISIBLE);
                break;
            default:
                textDesc.setText(R.string.not_valid_openturnkey);
                iconFail.setVisibility(View.VISIBLE);
        }

        new CountDownTimer(SHOW_RESULT_DELAY, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                cancelButton.callOnClick();
            }
        }.start();
    }

    public void setOnCanelListener (dialogReadOtkListener listener) {
        dialogListner = listener;
    }

    public static void extendCancelTimer() {
        disableCancelTimer();
        setCancelTimer();
    }

    public static void disableCancelTimer() {
        dialogTImer.cancel();
    }

    private static void setCancelTimer() {
        dialogTImer = new CountDownTimer(DIALOG_LIFETIME, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                cancelButton.callOnClick();
            }
        }.start();
    }

    public interface dialogReadOtkListener {
        void onCancel();
    }
}
