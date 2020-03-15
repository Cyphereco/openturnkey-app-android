package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkRequest;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.QRCodeUtils;

import java.util.Objects;

public class FragmentOtk extends FragmentExtendOtkViewPage {

    private final int AUTO_DISMISS_MILLIS = 30 * 1000;
    private static CountDownTimer timerRequestDismiss = null;
    private TextView tvRequestDesc;
    private CheckBox cbUsePin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_openturnkey, container, false);

        Button btnNfcRead = view.findViewById(R.id.btn_nfc_read);
        tvRequestDesc = view.findViewById(R.id.text_request_desc);
        cbUsePin = view.findViewById(R.id.otk_request_use_pin);

        btnNfcRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissTimer();
                if (hasRequest()) {
                    final OtkRequest request = peekRequest();
                    if ((cbUsePin.getVisibility() == View.VISIBLE && cbUsePin.isChecked())
                            || request.getCommand().equals(Command.UNLOCK.toString())) {
                        DialogAuthByPin dialogAuthByPin = new DialogAuthByPin();
                        dialogAuthByPin.setListener(new DialogAuthByPin.DialogAuthByPinListener() {
                            @Override
                            public void setPin(String pin) {
                                request.setPin(pin);
                                DialogReadOtk dialogReadOtk = new DialogReadOtk();
                                dialogReadOtk.setOnCanelListener(new DialogReadOtk.dialogReadOtkListener() {
                                    @Override
                                    public void onCancel() {
                                        clearRequest();
                                    }
                                });
                                assert getFragmentManager() != null;
                                dialogReadOtk.show(getFragmentManager(), "ReadOtk");
                            }
                        });
                        assert getFragmentManager() != null;
                        dialogAuthByPin.show(getFragmentManager(), "AuthByPin");
                        return;
                    }
                }

                DialogReadOtk dialogReadOtk = new DialogReadOtk();
                dialogReadOtk.setOnCanelListener(new DialogReadOtk.dialogReadOtkListener() {
                    @Override
                    public void onCancel() {
                        clearRequest();
                    }
                });
                assert getFragmentManager() != null;
                dialogReadOtk.show(getFragmentManager(), "ReadOtk");
            }
        });
        return view;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (hasRequest()) {
            OtkRequest request = peekRequest();
            if (peekRequest().getSessionId().length() > 0) {
                // the request has been sent
                if (request.getCommand().equals(Command.RESET.toString())) {
                    // a reset command needs no response, when it's sent, the request is completed
                    DialogReadOtk.updateReadOtkStatus(DialogReadOtk.READ_SUCCESS);
                    showStatusDialog(getString(R.string.reset_success), getString(R.string.reset_step_intro));
                }
            }
        }
    }

    @Override
    public void onOtkDataPosted(OtkData otkData) {
        super.onOtkDataPosted(otkData);

        // process received otk data
        if (otkData != null) {
            Intent intent = null;

            if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_NA) {
                // no particular request command, display general information
                intent = new Intent(getContext(), ActivityOpenturnkeyInfo.class);
            } else {
                // otkData contains request result, process the result according to the request
                Command cmd = otkData.getOtkState().getCommand();

                if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_SUCCESS) {
                    if (cmd == Command.SHOW_KEY) {
                        intent = new Intent(getContext(), ActivityKeyInformation.class);
                    } else if (cmd == Command.EXPORT_WIF_KEY) {
                        final String keyInfo = otkData.getSessionData().getWIFKey();
                        final View v = View.inflate(getActivity(), R.layout.dialog_private_key_wif, null);
                        TextView tvKeyString = v.findViewById(R.id.textView_export_key_string);
                        ImageView ivQRCode = v.findViewById(R.id.imageView_export_key_qrcode);

                        tvKeyString.setText(keyInfo);
                        Bitmap bitmap = QRCodeUtils.encodeAsBitmap(keyInfo,
                                ivQRCode.getDrawable().getIntrinsicWidth(),
                                ivQRCode.getDrawable().getIntrinsicHeight());
                        ivQRCode.setImageBitmap(bitmap);
                        // copy button
                        ImageView copy = v.findViewById(R.id.wif_key_copy);
                        copy.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Context context = view.getContext();
                                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("wif_key", keyInfo);
                                if (clipboard != null) {
                                    clipboard.setPrimaryClip(clip);
                                }
                                AlertPrompt.info(context, getString(R.string.data_copied));
                            }
                        });

                        new AlertDialog.Builder(getContext())
                                .setTitle(getString(R.string.export_private_key))
                                .setView(v)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    } else {
                        String msg;
                        switch (cmd) {
                            case UNLOCK:
                                msg = getString(R.string.unlock_success);
                                break;
                            case SET_KEY:
                                msg = getString(R.string.choose_key_success);
                                break;
                            case SET_PIN:
                                msg = getString(R.string.set_pin_success);
                                break;
                            case SET_NOTE:
                                msg = getString(R.string.write_note_success);
                                break;
                            case CANCEL:
                                msg = getString(R.string.operation_cancelled);
                                break;
                            case RESET:
                                msg = getString(R.string.reset_success);
                                break;
                            default:
                                msg = "Request completed.";
                        }
                        AlertPrompt.info(getContext(), msg);
                    }
                } else {
                    AlertPrompt.alert(getContext(), getString(R.string.request_fail) +
                            "\n" + getString(R.string.reason) + ": " +
                            parseFailureReason(otkData.getFailureReason()));
                }
            }

            if (intent != null) {
                intent.putExtra(MainActivity.KEY_OTK_DATA, otkData);
                // we don't need result here, we called startActivityForResult
                // only to prevent a bug, that we user press back button
                // the app closed instead of return to MainActivity,
                // when it return from pause state
                startActivityForResult(intent, 0);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.REQUEST_CODE_CHOOSE_KEY) {
            if (data == null) return;
            String keyPath = data.getStringExtra(ActivityChooseKey.KEY_PATH);
            if (keyPath != null && keyPath.length() > 0) {
                String[] strList = keyPath.split(",");
                if (strList.length == 5) {
                    cbUsePin.setVisibility(View.VISIBLE);
                    tvRequestDesc.setText(R.string.choose_key);
                    pushRequest(new OtkRequest(Command.SET_KEY.toString(), keyPath));
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        menu.clear();
        inflater.inflate(R.menu.menu_openturnkey, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int optionId = item.getItemId();
        clearRequest();

        cbUsePin.setVisibility(View.INVISIBLE);
        cbUsePin.setChecked(false);

        DialogConfirmationRequest dialog = new DialogConfirmationRequest(
                Objects.requireNonNull(getContext()),
                getString(R.string.warning),
                "",
                true);

        switch (optionId) {
            case R.id.menu_openturnkey_set_pin:
                dialog.setMessage(getString(R.string.pin_code_warning_message));
                dialog.setConfirmedButton(getString(R.string.understood), new DialogConfirmationRequest.OnConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        DialogSetPIN dialog = new DialogSetPIN();
                        dialog.setListener(new DialogSetPIN.DialogSetPINListener() {
                            @Override
                            public void setPin(String pin) {
                                cbUsePin.setVisibility(View.VISIBLE);
                                tvRequestDesc.setText(R.string.set_pin_code);
                                pushRequest(new OtkRequest(Command.SET_PIN.toString(), pin));
                            }
                        });
                        dialog.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(), "dialog");
                    }
                });
                dialog.show();
                break;
            case R.id.menu_openturnkey_get_key:
                dialog.setMessage(getString(R.string.full_pubkey_info_warning));
                dialog.setConfirmedButton(getString(R.string.understood), new DialogConfirmationRequest.OnConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        cbUsePin.setVisibility(View.VISIBLE);
                        tvRequestDesc.setText(R.string.full_pubkey_information);
                        pushRequest(new OtkRequest(Command.SHOW_KEY.toString()));
                    }
                });
                dialog.show();
                break;
            case R.id.menu_openturnkey_set_note:
                DialogAddNote dialogAddNote = new DialogAddNote();
                dialogAddNote.setListener(new DialogAddNote.DialogAddNoteListener() {
                    @Override
                    public void addNote(String note) {
                        cbUsePin.setVisibility(View.VISIBLE);
                        tvRequestDesc.setText(R.string.write_note);
                        pushRequest(new OtkRequest(Command.SET_NOTE.toString(), note));
                    }
                });
                dialogAddNote.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(), "dialog");
                break;
            case R.id.menu_openturnkey_sign_message:
                intent = new Intent(getContext(), ActivitySignValidateMessage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                // we don't need result here, we called startActivityForResult
                // only to prevent a bug, that we user press back button
                // the app closed instead of return to MainActivity,
                // when it return from pause state
                startActivityForResult(intent, 0);
                break;
            case R.id.menu_openturnkey_choose_key:
                dialog.setMessage(getString(R.string.choose_key_warning_message));
                dialog.setConfirmedButton(getString(R.string.understood), new DialogConfirmationRequest.OnConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        Intent intent = new Intent(getContext(), ActivityChooseKey.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivityForResult(intent, MainActivity.REQUEST_CODE_CHOOSE_KEY);
                        // following process will be handled in onActivityResult, depending on
                        // whether valid key path is entered.
                    }
                });
                dialog.show();
                break;
            case R.id.menu_openturnkey_unlock:
                dialog.setMessage(getString(R.string.unlock_warning));
                dialog.setConfirmedButton(getString(R.string.understood), new DialogConfirmationRequest.OnConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        tvRequestDesc.setText(R.string.unlock);
                        pushRequest(new OtkRequest(Command.UNLOCK.toString()));
                    }
                });
                dialog.show();
                break;
            case R.id.menu_openturnkey_reset:
                dialog.setMessage(getString(R.string.reset_warning_message));
                dialog.setConfirmedButton(getString(R.string.understood), new DialogConfirmationRequest.OnConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        tvRequestDesc.setText(R.string.reset);
                        pushRequest(new OtkRequest(Command.RESET.toString()));
                    }
                });
                dialog.show();
                break;
            case R.id.menu_openturnkey_export_wif_key:
                dialog.setMessage(getString(R.string.export_wif_warning_message));
                dialog.setConfirmedButton(getString(R.string.understood), new DialogConfirmationRequest.OnConfirmedListener() {
                    @Override
                    public void onConfirmed() {
                        tvRequestDesc.setText(R.string.export_private_key);
                        pushRequest(new OtkRequest(Command.EXPORT_WIF_KEY.toString()));
                    }
                });
                dialog.show();
                break;
            default:
                tvRequestDesc.setText(R.string.read_general_information);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageSelected() {
        super.onPageSelected();
    }

    @Override
    public void onPageUnselected() {
        super.onPageUnselected();

        logger.debug("clear request");
        clearRequest();
    }

    private void dismissTimer() {
        if (timerRequestDismiss != null) {
            timerRequestDismiss.cancel();
            timerRequestDismiss = null;
        }
    }

    @Override
    protected void clearRequest() {
        super.clearRequest();
        cbUsePin.setVisibility(View.INVISIBLE);
        cbUsePin.setChecked(false);
        tvRequestDesc.setText(R.string.read_general_information);
    }

    @Override
    protected void pushRequest(OtkRequest request) {
        super.pushRequest(request);
        // set a timer to dismiss request automatically
        timerRequestDismiss = new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                clearRequest();
            }
        };
        timerRequestDismiss.start();
    }

    public String parseFailureReason(String desc) {
        if (desc == null || desc.equals("")) {
            return getString(R.string.communication_error);
        }
        switch (desc) {
            case "C0":
                return getString(R.string.session_timeout);
            case "C1":
                return getString(R.string.auth_failed);
            case "C3":
                return getString(R.string.invalid_params);
            case "C4":
                return getString(R.string.missing_params);
            case "C7":
                return getString(R.string.pin_unset);
            case "00":
            case "C2":
            case "FF":
                return getString(R.string.invalid_command);
            default:
                return desc;
        }
    }

    private void showStatusDialog(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());

        alertDialogBuilder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show();
    }
}
