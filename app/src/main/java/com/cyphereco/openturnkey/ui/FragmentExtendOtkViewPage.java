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
    private boolean isSelected = false;

    protected DialogReadOtk dialogReadOtk;

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

                if (hasRequest()) {
                    request = peekRequest();
                    // can be override by inherited class to modify request, such as
                    // if otk is not authorized, app can prompt a pin dialog and set pin
                    preRequestSend(request, otkData);

                    // request being canceled, do nothing
                    if (!request.isValid()) return;

                    /*
                     * If a request has a session Id, the request have been delivered
                     * to an openturnkey and expecting a request result.
                     */
                    if (request.getSessionId().length() > 0) {
                        logger.info("Waiting for request result");

                        // Sanity check on the otkData
                        if (!otkData.getSessionData().getSessionId().equals(request.getSessionId())) {
                            /*
                             If session id mismatches, error occurs, should quit request to avoid
                             suspicious hack.
                             */
                            logger.error("Incorrect openturnkey");
                            clearRequest();
                            dialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

                            delayProcessAfterReadOtkEnded(new PostReadOtkHandler() {
                                @Override
                                public void postProcess() {
                                    AlertPrompt.alert(getContext(), getString(R.string.not_openturnkey));
                                }
                            });
                        } else {
                            // otkData should contains request result
                            if (otkData.getOtkState().getExecutionState() == OtkState.ExecutionState.NFC_CMD_EXEC_SUCCESS) {
                                // process valid OpenTurnKey data
                                if (!request.hasMore()) {
                                    // no more request followed, request execution completed
                                    clearRequest();
                                    // Notify the DialogReadOtk of the parsing result and close the dialog.
                                    dialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.READ_SUCCESS);

                                    delayProcessAfterReadOtkEnded(new PostReadOtkHandler() {
                                        @Override
                                        public void postProcess() {
                                            onOtkDataPosted(otkData);
                                        }
                                    });
                                } else {
                                    // more request to process
                                    onOtkDataPosted(otkData);
                                    pollRequest();
                                    enableReadOtk();
                                    dialogReadOtk.extendCancelTimer();
                                    dialogReadOtk.updateReadOtkDesc(getString(R.string.reapproach_openturnkey));
                                }
                            } else {
                                // request failed, clear request and prompt failure reason
                                clearRequest();
                                dialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

                                delayProcessAfterReadOtkEnded(new PostReadOtkHandler() {
                                    @Override
                                    public void postProcess() {
                                        AlertPrompt.alert(getContext(), getString(R.string.request_fail) +
                                                "\n" + getString(R.string.reason) + ": " +
                                                parseFailureReason(otkData.getFailureReason()));
                                    }
                                });
                            }
                        }
                    } else {
                        // Pending request has not been delivered, prepare to send.

                        // Check if OpenTurnKey is locked to accept authentication.
                        if (otkData.getOtkState().getLockState() == OtkState.LockState.UNLOCKED) {
                            // OpenTurnKey is not locked, request cannot be made
                            clearRequest();
                            dialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

                            delayProcessAfterReadOtkEnded(new PostReadOtkHandler() {
                                @Override
                                public void postProcess() {
                                    AlertPrompt.alert(getContext(), getString(R.string.request_fail) +
                                            "\n" + getString(R.string.reason) + ": " +
                                            parseFailureReason(otkData.getFailureReason()));
                                }
                            });
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

                            logger.info("Session ID mismatched {} != {}, request is not sent.",
                                    sessId, otkData.getSessionData().getSessionId());
                            clearRequest();
                            dialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.REQUEST_FAIL);

                            delayProcessAfterReadOtkEnded(new PostReadOtkHandler() {
                                @Override
                                public void postProcess() {
                                    AlertPrompt.alert(getContext(), getString(R.string.not_openturnkey));

                                }
                            });
                        } else {
                            /*
                             Request delivered, the current otkData is not useful result.
                             Set otkData to null and waiting for process request result in
                             the next intent.
                             */
                            enableReadOtk();
                            dialogReadOtk.extendCancelTimer();
                            dialogReadOtk.updateReadOtkDesc(getString(R.string.processing_request));
                        }
                    }
                } else {
                    // no request, just read openturnkey information
                    dialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.READ_SUCCESS);

                    delayProcessAfterReadOtkEnded(new PostReadOtkHandler() {
                        @Override
                        public void postProcess() {
                            // dispatch otkData
                            onOtkDataPosted(otkData);
                        }
                    });
                }
            } else {
                logger.info("Not a valid OpenTurnKey");
                // Notify the DialogReadOtk of the parsing result and close the dialog.
                dialogReadOtk.endingDialogReadOtkWithReason(DialogReadOtk.NOT_OPENTURNKEY);
                delayProcessAfterReadOtkEnded(new PostReadOtkHandler() {
                    @Override
                    public void postProcess() {
                        AlertPrompt.info(getContext(), getString(R.string.not_openturnkey));
                    }
                });
            }
        }
    }

    public void onOtkDataPosted(OtkData data) {
        logger.debug("otkData posted, accessible within the class as otkData");
        otkData = data;
    }

    public void onPageSelected() {
        logger.debug("Page selected");
        isSelected = true;
    }

    public void onPageUnselected() {
        logger.debug("Page unselected");
        isSelected = false;
    }

    public boolean isSelected() {
        return isSelected;
    }

    protected void clearRequest() {
        logger.debug("clear all requests");
        while (numOfRequest() > 0) pollRequest();
    }

    protected void pushRequest(OtkRequest request) {
        logger.debug("pushed request: {}", request);
        otkRequestQueue.add(request);
    }

    protected OtkRequest pollRequest() {
        OtkRequest request = otkRequestQueue.poll();
        assert request != null;
        logger.debug("polled request: {}", request);
        return request;
    }

    protected OtkRequest peekRequest() {
        OtkRequest request = otkRequestQueue.peek();
        assert request != null;
        logger.debug("peek request: {}", request);
        return request;
    }

    protected int numOfRequest() {
        return otkRequestQueue.size();
    }

    protected boolean hasRequest() {
        return numOfRequest() > 0;
    }

    protected void preRequestSend(OtkRequest request, OtkData otkData) {
        logger.debug("request/otkData before modification:\n{}\n{}", request, otkData);
    }

    protected void enableReadOtk() {
        MainActivity.enableReadOtk();
        logger.debug("ReadOtk enabled");
    }

    protected void disableReadOtk() {
        MainActivity.disableReadOtk();
        logger.debug("ReadOtk disabled");
    }

    protected void showDialogReadOtk(String title, String desc) {
        dialogReadOtk = new DialogReadOtk()
                .updateReadOtkTitle(title)
                .updateReadOtkDesc(desc)
                .setOnCanelListener(new DialogReadOtk.DialogReadOtkListener() {
                    @Override
                    public void onCancel() {
                        clearRequest();
                    }
                });
        dialogReadOtk.show(getFragmentManager(), "ReadOtk");
    }

    protected String parseFailureReason(String desc) {
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

    protected void delayProcessAfterReadOtkEnded(final PostReadOtkHandler handler) {
        /* There is an otk show result delay in DialogReadOtk,
            to start to a new activity, or prompt message, wait until the dialog closed.
         */
        new CountDownTimer(DialogReadOtk.SHOW_RESULT_DELAY +
                DialogReadOtk.DISMISS_ANIMATION_TIME, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                handler.postProcess();
            }
        }.start();
    }

    protected interface PostReadOtkHandler {
        void postProcess();
    }
}
