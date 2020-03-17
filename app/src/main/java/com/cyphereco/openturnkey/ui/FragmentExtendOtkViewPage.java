package com.cyphereco.openturnkey.ui;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import com.cyphereco.openturnkey.R;
import com.cyphereco.openturnkey.core.NfcHandler;
import com.cyphereco.openturnkey.core.OtkData;
import com.cyphereco.openturnkey.core.protocol.OtkRequest;
import com.cyphereco.openturnkey.core.protocol.OtkState;
import com.cyphereco.openturnkey.utils.AlertPrompt;
import com.cyphereco.openturnkey.utils.Log4jHelper;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.Queue;

public class FragmentExtendOtkViewPage extends Fragment {
    public OtkData otkData = null;
    private Queue<OtkRequest> otkRequestQueue = new LinkedList<>();
    protected Logger logger;

    public FragmentExtendOtkViewPage() {
        String TAG = this.getClass().getSimpleName();
        logger = Log4jHelper.getLogger(TAG);
    }

    public void onNewIntent(Intent intent) {
        logger.debug("New Intent");

        String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            logger.info("Found NFC tag!!");
            final OtkData otkData = NfcHandler.parseIntent(intent);

            if (otkData != null) {
                OtkRequest request;
                logger.debug("Found OpenTurnKey:\nSession#{} : {}{}",
                        otkData.getSessionData().getSessionId(),
                        otkData.getSessionData().getAddress(),
                        otkData.getOtkState().toString()
                );

                // disableReadOtk to avoid new intent breaking unfinished processing
                disableReadOtk();
//                DialogReadOtk.updateReadOtkDesc("Reading...");

                /*
                 If the otkRequestQueue is not empty, there is a request pending.
                 Check if public key and session id matched current session.
                 */
                if (hasRequest()) {
                    request = peekRequest();
                    // can be override by inherited class to modify request, such as
                    // if otk is not authorized, app can prompt a pin dialog and set pin
                    modifyRequestAfterReadOtkBeforeSubmit(request, otkData);

                    /*
                     If a request has a session Id and otk address, the request
                     must have been delivered to an openturnkey and expecting a request result.
                     */
                    if (request.getSessionId().length() > 0) {
                        logger.info("Waiting for request result");

                        // Sanity check on the otkData
                        if (!otkData.getSessionData().getSessionId().equals(request.getSessionId())) {
                            /*
                             Request has been delivered, intent should contain request result.
                             Either success or fail。

                             If session id mismatches, error occurs, should quit request to avoid
                             suspicious hack.
                             */
                            logger.error("Incorrect openturnkey");
                            clearRequest();
                            DialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

                            new CountDownTimer(DialogReadOtk.SHOW_RESULT_DELAY +
                                    DialogReadOtk.DISMISS_ANIMATION_TIME, 1000) {
                                public void onTick(long millisUntilFinished) {
                                }

                                public void onFinish() {
                                    AlertPrompt.alert(getContext(), getString(R.string.not_openturnkey));
                                }
                            }.start();
                        }
                        else {
                            // otkData should contains request result
                            if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_SUCCESS) {
                                // request executed successful
                                // process valid OpenTurnKey data
                                logger.info(otkData.toString());
                                if (!request.hasMore()) {
                                    // no more request followed, request execution completed
                                    clearRequest();
                                    // Notify the DialogReadOtk of the parsing result and close the dialog.
                                    DialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.READ_SUCCESS);

                                    /* There is an otk show result delay in DialogReadOtk,
                                        to start to a new activity, wait until the dialog closed.
                                     */
                                    new CountDownTimer(DialogReadOtk.SHOW_RESULT_DELAY +
                                            DialogReadOtk.DISMISS_ANIMATION_TIME, 1000) {
                                        public void onTick(long millisUntilFinished) {
                                        }

                                        public void onFinish() {
                                            onOtkDataPosted(otkData);
                                        }
                                    }.start();
                                }
                                else {
                                    // more request to process
                                    pollRequest();
                                    onOtkDataPosted(otkData);
                                    enableReadOtk();
                                    DialogReadOtk.extendCancelTimer();
                                    DialogReadOtk.updateReadOtkDesc("More requests to be processed, move away OpenTurnKey then approach again.");
                                }
                            }
                            else {
                                // request failed, clear request and prompt failure reason
                                clearRequest();
                                DialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

                                new CountDownTimer(DialogReadOtk.SHOW_RESULT_DELAY +
                                        DialogReadOtk.DISMISS_ANIMATION_TIME, 1000) {
                                    public void onTick(long millisUntilFinished) {
                                    }

                                    public void onFinish() {
                                        AlertPrompt.alert(getContext(), getString(R.string.request_fail) +
                                                "\n" + getString(R.string.reason) + ": " +
                                                parseFailureReason(otkData.getFailureReason()));                                    }
                                }.start();
                            }
                        }
                    }
                    else {
                        // Pending request has not been delivered, prepare to send.

                        // Check if OpenTurnKey is locked to accept authentication.
                        if (otkData.getOtkState().getLockState() == OtkState.LockState.UNLOCKED) {
                            // OpenTurnKey is not locked, request cannot be made
                            clearRequest();
                            DialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

                            new CountDownTimer(DialogReadOtk.SHOW_RESULT_DELAY +
                                    DialogReadOtk.DISMISS_ANIMATION_TIME, 1000) {
                                public void onTick(long millisUntilFinished) {
                                }

                                public void onFinish() {
                                    AlertPrompt.alert(getContext(), getString(R.string.otk_is_not_locked));
                                }
                            }.start();
                            return;
                        }

                        /*
                         Set the session Id and otk address with the otkData we just parsed.
                         */
                        request.setSessionId(otkData.getSessionData().getSessionId());
                        request.setOtkAddress(otkData.getSessionData().getAddress());
                        String sessId = NfcHandler.sendRequest(intent, request);

                        if (!otkData.getSessionData().getSessionId().equals(sessId)) {
                            /*
                             Send request failed, most likely a communication error occurs.
                             Remove the session id and otk address and keep the request as a fresh one.
                             */

                            logger.info("Something wrong, request is not sent.");
                            clearRequest();
                            DialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

                            new CountDownTimer(DialogReadOtk.SHOW_RESULT_DELAY +
                                    DialogReadOtk.DISMISS_ANIMATION_TIME, 1000) {
                                public void onTick(long millisUntilFinished) {
                                }

                                public void onFinish() {
                                    AlertPrompt.alert(getContext(), getString(R.string.not_openturnkey));
                                }
                            }.start();
                        }
                        else {
                            /*
                             Request delivered, the current otkData is not useful result.
                             Set otkData to null and waiting for process request result in
                             the next intent.
                             */
                            enableReadOtk();
                            DialogReadOtk.extendCancelTimer();
                            DialogReadOtk.updateReadOtkDesc(getString(R.string.processing_request));
                        }
                    }
                }
                else {
                    DialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.READ_SUCCESS);

                                    /* There is an otk show result delay in DialogReadOtk,
                                        to start to a new activity, wait until the dialog closed.
                                     */
                    new CountDownTimer(DialogReadOtk.SHOW_RESULT_DELAY +
                            DialogReadOtk.DISMISS_ANIMATION_TIME, 1000) {
                        public void onTick(long millisUntilFinished) {
                        }

                        public void onFinish() {
                            onOtkDataPosted(otkData);
                        }
                    }.start();
                }
            }
            else {
                logger.info("Not a valid OpenTurnKey");
                // Notify the DialogReadOtk of the parsing result and close the dialog.
                DialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.NOT_OPENTURNKEY);
             }
        }
    }

    public void onOtkDataPosted(OtkData data) {
        logger.debug("otkData posted, accessible within the class as otkData");
        otkData = data;
    }

    public void onPageSelected() {
        logger.debug("Page selected");
    }

    public void onPageUnselected() {
        logger.debug("Page unselected");
    }

    protected void clearRequest() {
        while (numOfRequest() > 0) pollRequest();
    }

    protected void pushRequest(OtkRequest request) {
        logger.debug("pushed request: {}", request.toString());
        otkRequestQueue.add(request);
    }

    protected OtkRequest pollRequest() {
        OtkRequest request = otkRequestQueue.poll();
        assert request != null;
        logger.debug("polled request: {}", request.toString());
        return request;
    }

    protected OtkRequest peekRequest() {
        OtkRequest request = otkRequestQueue.peek();
        assert request != null;
        logger.debug("peek request: {}", request.toString());
        return request;
    }

    protected int numOfRequest() {
        return otkRequestQueue.size();
    }

    protected boolean hasRequest() {
        return numOfRequest() > 0;
    }

    protected void modifyRequestAfterReadOtkBeforeSubmit(OtkRequest request, OtkData otkData) {
        logger.debug("Modify request after received OtkData and before submit");
    }

    protected void enableReadOtk() {
        MainActivity.enableReadOtk();
        logger.debug("ReadOtk enabled");
    }

    protected void disableReadOtk() {
        MainActivity.disableReadOtk();
        logger.debug("ReadOtk disabled");
    }

    private String parseFailureReason(String desc) {
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
}
