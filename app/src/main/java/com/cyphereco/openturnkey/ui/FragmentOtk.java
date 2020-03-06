package com.cyphereco.openturnkey.ui;

import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.Otk;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.protocol.Command;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.slf4j.Logger;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.cyphereco.openturnkey.ui.MainActivity.KEY_OTK_DATA;

public class FragmentOtk extends FragmentExtOtkData {
    public static final String TAG = FragmentOtk.class.getSimpleName();
    Logger logger = Log4jHelper.getLogger(TAG);

    // Cancel timeout for pay
    private final int AUTO_DISMISS_MILLIS = 30 * 1000;

    private FragmentOtkListener mListener;
    private static final String ARG_OPERATION = "operation";
    private Otk.Operation mOp;
    private CountDownTimer mCancelTimer = null;

    public static FragmentOtk newInstance(Otk.Operation op) {
        FragmentOtk fragment = new FragmentOtk();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OPERATION, op);
        fragment.setArguments(args);
        return fragment;
    }

    private void updateOp(View view) {
        final TextView tv;
        tv = view.findViewById(R.id.text_nfc_comm_type);
        // Cancel button
        final Button btn = view.findViewById(R.id.button_otk_cancel);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    if (mCancelTimer != null) {
                        mCancelTimer.cancel();
                    }
                    mListener.onCancelButtonClick();
                }
            }
        });

        if (mOp == Otk.Operation.OTK_OP_SIGN_PAYMENT) {
            tv.setText(R.string.sign_payment);
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopCancelTimer();
                    if (mListener != null) {
                        if (mCancelTimer != null) {
                            mCancelTimer.cancel();
                        }
                        mListener.onCancelButtonClick();
                    }
                }
            });
        } else if (mOp == Otk.Operation.OTK_OP_GET_RECIPIENT_ADDRESS) {
            tv.setText(R.string.get_recipient_address);
            btn.setVisibility(View.VISIBLE);

        } else if (mOp == Otk.Operation.OTK_OP_GET_KEY) {
            tv.setText(R.string.full_pubkey_information);
            btn.setVisibility(View.VISIBLE);
        } else if (mOp == Otk.Operation.OTK_OP_SET_PIN_CODE) {
            tv.setText(R.string.set_pin_code);
            btn.setVisibility(View.VISIBLE);
        } else if (mOp == Otk.Operation.OTK_OP_WRITE_NOTE) {
            tv.setText(R.string.write_note);
            btn.setVisibility(View.VISIBLE);
        } else if (mOp == Otk.Operation.OTK_OP_UNLOCK) {
            tv.setText(R.string.unlock);
            btn.setVisibility(View.VISIBLE);
        } else if (mOp == Otk.Operation.OTK_OP_SIGN_MESSAGE) {
            tv.setText(R.string.sign_message);
            btn.setVisibility(View.VISIBLE);
        } else if (mOp == Otk.Operation.OTK_OP_CHOOSE_KEY) {
            tv.setText(R.string.set_key);
            btn.setVisibility(View.VISIBLE);
        } else if (mOp == Otk.Operation.OTK_OP_RESET) {
            tv.setText(R.string.reset);
            btn.setVisibility(View.VISIBLE);
        } else if (mOp == Otk.Operation.OTK_OP_EXPORT_WIF_KEY) {
            tv.setText(R.string.export_private_key);
            btn.setVisibility(View.VISIBLE);
        } else {
            btn.setVisibility(View.INVISIBLE);
            // Default is read general info
            tv.setText(R.string.read_general_information);
        }

        // Stop it if it's running
        if (mCancelTimer != null) {
            mCancelTimer.cancel();
        }

        if ((mOp != Otk.Operation.OTK_OP_READ_GENERAL_INFO) &&
                (mOp != Otk.Operation.OTK_OP_NONE)) {
            mCancelTimer = new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    btn.setText(String.format(
                            Locale.getDefault(), "%s (%d)",
                            getString(R.string.cancel),
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1 //add one so it never displays zero
                    ));
                }

                public void onFinish() {
                    if (mListener != null) {
                        mListener.onCancelTimeout();
                        btn.setVisibility(View.INVISIBLE);
                        // Default is read general info
                        tv.setText(R.string.read_general_information);
                    }
                }
            };
            mCancelTimer.start();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            mOp = (Otk.Operation) getArguments().getSerializable(ARG_OPERATION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_openturnkey, container, false);

        updateOp(view);

        Button btnNfcRead = view.findViewById(R.id.btn_nfc_read);

        btnNfcRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.readOtk();
//                DialogReadOtk dialogReadOtk = new DialogReadOtk();
//                dialogReadOtk.show(getFragmentManager(), "ReadOtk");
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        logger.info("FragmentOtk attached");
        logger.info("TAG={}", TAG);
        if (context instanceof FragmentOtkListener) {
            mListener = (FragmentOtkListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        logger.info("FragmentOtk detached");

        mListener = null;
        if (mCancelTimer != null) {
            mCancelTimer.cancel();
        }
    }

    public void stopCancelTimer() {
        if (mCancelTimer != null) {
            mCancelTimer.cancel();
        }
        Button btn = getView().findViewById(R.id.button_otk_cancel);
        btn.setText(R.string.cancel);
    }

    public void updateOperation(Otk.Operation op) {
        mOp = op;
        updateOp(getView());
    }

    public interface FragmentOtkListener {
        void onCancelButtonClick();

        void onCancelTimeout();
    }

    @Override
    public void postOtkData(OtkData otkData) {
        super.postOtkData(otkData);

        // process received otk data
        if (otkData != null) {
            Intent intent = null;

            if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_NA) {
                // read OTK general information, must be getting the OTK address
                intent = new Intent(getContext(), ActivityOpenturnkeyInfo.class);
            }
            else {
                // otkData contains request result, process the result according to the reqeust
                Command cmd = otkData.getOtkState().getCommand();

                if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_SUCCESS) {
                    if (cmd == Command.SHOW_KEY) {
                        intent = new Intent(getContext(), ActivityKeyInformation.class);
                    }
                    else if (cmd == Command.EXPORT_WIF_KEY) {

                    }
                    else {
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
                }
                else {
                    AlertPrompt.alert(getContext(),"Request failed.\nReason:\n" + otkData.getFailureReason());
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
        logger.debug("opt: {}", optionId);
        return super.onOptionsItemSelected(item);
    }
}
