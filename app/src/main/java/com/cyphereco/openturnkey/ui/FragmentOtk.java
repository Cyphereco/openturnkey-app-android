package com.cyphereco.openturnkey.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.OtkEvent;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkCommand;
import com.cyphereco.openturnkey.core.protocol.OtkRequest;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.QRCodeUtils;

import static com.cyphereco.openturnkey.ui.MainActivity.KEY_OTK_DATA;

public class FragmentOtk extends FragmentExtendOtkViewPage {

    private final int AUTO_DISMISS_MILLIS = 30 * 1000;
    private static CountDownTimer timerRequestDismiss = null;
    private static TextView tvRequestDesc;

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

        btnNfcRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissTimer();
                DialogReadOtk dialogReadOtk = new DialogReadOtk();
                dialogReadOtk.setOnCanelListener(new DialogReadOtk.dialogReadOtkListener() {
                    @Override
                    public void onCancel() {
                        clearRequest();
                    }
                });
                dialogReadOtk.show(getFragmentManager(), "ReadOtk");
            }
        });
        return view;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (hasRequest() && peekRequest().getCommand().equals(Command.RESET.toString()) &&
        peekRequest().getSessionId().length() > 0) {
            DialogReadOtk.updateReadOtkStatus(DialogReadOtk.READ_SUCCESS);
            showStatusDialog(getString(R.string.reset_success), getString(R.string.reset_step_intro));
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
                // otkData contains request result, process the result according to the reqeust
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
                                AlertPrompt.info(context, getString(R.string.copy));
                            }
                        });

                        new AlertDialog.Builder(getContext())
                                .setTitle(getString(R.string.export_private_key))
                                .setView(v)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
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
                intent.putExtra(KEY_OTK_DATA, otkData);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        menu.clear();
        inflater.inflate(R.menu.menu_openturnkey, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int optionId = item.getItemId();
        clearRequest();

        switch (optionId) {
            case R.id.menu_openturnkey_set_pin:
//                dialogSetPinReminder();
//                dialogSetPIN();
                tvRequestDesc.setText(R.string.set_pin_code);
                pushRequest(new OtkRequest(Command.SET_PIN.toString(), "99999999"));
                break;
            case R.id.menu_openturnkey_get_key:
//                dialogShowPublicKeyReminder();
                tvRequestDesc.setText(R.string.full_pubkey_information);
                pushRequest(new OtkRequest(Command.SHOW_KEY.toString()));
                break;
            case R.id.menu_openturnkey_set_note:
                // dialogSetNote();
                tvRequestDesc.setText(R.string.write_note);
                pushRequest(new OtkRequest(Command.SET_NOTE.toString(), "Hello"));
                break;
            case R.id.menu_openturnkey_sign_message:
                Intent intent = new Intent(getContext(), ActivitySignValidateMessage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                break;
            case R.id.menu_openturnkey_choose_key:

                tvRequestDesc.setText(R.string.choose_key);
                pushRequest(new OtkRequest(Command.SET_PIN.toString(), "2,4,6,8,10"));
                break;
            case R.id.menu_openturnkey_unlock:
                tvRequestDesc.setText(R.string.unlock);
                pushRequest(new OtkRequest(Command.UNLOCK.toString()));
                break;
            case R.id.menu_openturnkey_reset:
                tvRequestDesc.setText(R.string.reset);
                pushRequest(new OtkRequest(Command.RESET.toString()));
                break;
            case R.id.menu_openturnkey_export_wif_key:
                tvRequestDesc.setText(R.string.export_private_key);
                pushRequest(new OtkRequest(Command.EXPORT_WIF_KEY.toString()));
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

    private void dialogAddNote() {
        DialogAddNote dialog = new DialogAddNote();
        dialog.show(getActivity().getSupportFragmentManager(), "dialog");
    }

    private void dialogSetPIN() {
        DialogSetPIN dialog = new DialogSetPIN();
        dialog.show(getActivity().getSupportFragmentManager(), "dialog");
    }

    public String parseFailureReason(String desc) {
        if (desc == null || desc.equals("")) {
            return getString(R.string.unknown_reason);
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
