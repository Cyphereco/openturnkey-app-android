package com.cyphereco.openturnkey.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DialogReadOtk extends AppCompatDialogFragment {
    public static final int DIALOG_LIFETIME = 30000;
    public static final int SHOW_RESULT_DELAY = 2000;
    public static final int DISMISS_ANIMATION_TIME = 250;

    private Button cancelButton;
    private TextView textTitle;
    private TextView textDesc;
    private ImageView iconHint;
    private ImageView iconSuccess;
    private ImageView iconFail;
    private String customizeTitle;

    private static DialogReadOtkListener dialogListner;
    private static CountDownTimer dialogTImer;

    final public static int NOT_OPENTURNKEY = 0;
    final public static int READ_SUCCESS = 1;
    final public static int REQUEST_FAIL = 2;

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_read_otk, null);

        final Dialog dialog = new AlertDialog.Builder(getActivity()).setView(view).create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.ShowReadOtkAnimation;
        dialog.setCanceledOnTouchOutside(false);

        textTitle = view.findViewById(R.id.text_read_otk_title);
        textDesc = view.findViewById(R.id.text_read_otk_desc);
        iconHint = view.findViewById(R.id.image_read_otk_hint);
        iconSuccess = view.findViewById(R.id.image_read_otk_success);
        iconFail = view.findViewById(R.id.image_read_otk_fail);
        cancelButton = view.findViewById(R.id.btn_cancel_read_otk);

        if (customizeTitle != null && customizeTitle.length() > 0) textTitle.setText(customizeTitle);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogTImer.cancel();
                if (getDialog() != null) dismissAnimation(Objects.requireNonNull(getDialog().getWindow()).getDecorView());
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

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        // turn off the nfc reading for otk
        MainActivity.disableReadOtk();
        // call onCancel handler
        if (dialogListner != null) dialogListner.onCancel();
    }

    private void setCancelTimer() {
        dialogTImer = new CountDownTimer(DIALOG_LIFETIME, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (cancelButton != null) {
                    // trigger normal cancel as cancel button clicked, exit with animation
                    cancelButton.callOnClick();
                } else {
                    // dialog has been closed, finish cancel operations
                    dialogTImer.cancel();
                }
            }
        }.start();
    }

    private void dismissAnimation(View view) {
        ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat("translationY", 0, 1200f),
                PropertyValuesHolder.ofFloat("alpha", 1.0f, 0.0f));
        final DialogReadOtk dialog = this;
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dialog.dismiss();
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

    public DialogReadOtk updateReadOtkTitle(String title) {
        customizeTitle = title;
        return this;
    }

    public DialogReadOtk updateReadOtkDesc(String desc) {
        if (desc != null && desc.length() > 0) textDesc.setText(desc);
        return this;
    }

    public DialogReadOtk extendCancelTimer() {
        disableCancelTimer();
        setCancelTimer();
        return this;
    }

    public DialogReadOtk disableCancelTimer() {
        dialogTImer.cancel();
        return this;
    }

    public DialogReadOtk setOnCanelListener(DialogReadOtkListener listener) {
        dialogListner = listener;
        return this;
    }

    public void endingDialogReadOtkWithReason(final int flag) {
        disableCancelTimer();

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
                if (READ_SUCCESS == flag) {
                    // read success, dismiss normally without calling onCancel listener
                    dialogListner = null;
                }
                dialogTImer.cancel();
                cancelButton.callOnClick();
            }
        }.start();
    }

    public interface DialogReadOtkListener {
        void onCancel();
    }
}
