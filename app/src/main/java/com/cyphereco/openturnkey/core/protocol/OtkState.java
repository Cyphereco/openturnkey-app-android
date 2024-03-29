package com.cyphereco.openturnkey.core.protocol;

import com.cyphereco.openturnkey.utils.Log4jHelper;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.Locale;

public class OtkState implements Serializable {
    public static final String TAG = OtkState.class.getSimpleName();
    private static Logger logger = Log4jHelper.getLogger(TAG);

    public enum LockState {
        INVALID("-1"),
        UNLOCKED("00"),
        LOCKED("01"),
        AUTHORIZED("02");

        private final String value;

        LockState(String s) {
            value = s;
        }

        @NotNull
        public String toString() {
            return value;
        }
    }

    public enum ExecutionState {
        NFC_CMD_EXEC_NA("00"),
        NFC_CMD_EXEC_SUCCESS("01"),        /* 1, Command executed successfully. */
        NFC_CMD_EXEC_FAIL("02");           /* 2, Command executed failed. */
        private final String value;

        ExecutionState(String s) {
            value = s;
        }

        @NotNull
        public String toString() {
            return value;
        }
    }

    public enum FailureReason {
        NFC_REASON_INVALID("00"),  /*   0 / 0x00, Invalid,  For check purpose. */
        NFC_REASON_TIMEOUT("C0"),  /* 192 / 0xC0, Enroll fingerprint on OTK. */
        NFC_REASON_AUTH_FAILED("C1"),     /* 193 / 0xC1, Erase enrolled fingerprint and reset secure PIN to default, OTK (pre)authorization is required. */
        NFC_REASON_CMD_INVALID("C2"),     /* 194 / 0xC2, Present master/derivative extend keys and derivative path and secure PIN code, OTK (pre)authorization is required. */
        NFC_REASON_PARAM_INVALID("C3"),   /* 195 / 0xC3, Present master/derivative extend keys and derivative path and secure PIN code, OTK (pre)authorization is required. */
        NFC_REASON_PARAM_MISSING("C4"),   /* 196 / 0xC4, Present master/derivative extend keys and derivative path and secure PIN code, OTK (pre)authorization is required. */
        NFC_REASON_PIN_UNSET("C7");       /* 197 / 0xC7, PIN code is not set yet. */
        //        NFC_REASON_LAST("FF");
        private final String value;

        FailureReason(String v) {
            value = v;
        }

        public String getValue() {
            return value;
        }
//        public String getReasonString() {
//            return reason;
//        }
    }

    public OtkState(String stateStr) {
        // Lock state
        String code = stateStr.substring(0, 2);
        if (code.equalsIgnoreCase(LockState.UNLOCKED.toString())) {
            mLockState = LockState.UNLOCKED;
        } else if (code.equalsIgnoreCase(LockState.LOCKED.toString())) {
            mLockState = LockState.LOCKED;
        } else if (code.equalsIgnoreCase(LockState.AUTHORIZED.toString())) {
            mLockState = LockState.AUTHORIZED;
        } else {
            /* Should not be here. */
            logger.debug("Lock state is invalid.");
            mLockState = LockState.INVALID;
        }

        // Command exec state
        code = stateStr.substring(2, 4);
        if (code.equalsIgnoreCase(ExecutionState.NFC_CMD_EXEC_NA.toString())) {
            mNfcCmdExecSate = ExecutionState.NFC_CMD_EXEC_NA;
        } else if (code.equalsIgnoreCase(ExecutionState.NFC_CMD_EXEC_SUCCESS.toString())) {
            mNfcCmdExecSate = ExecutionState.NFC_CMD_EXEC_SUCCESS;
        } else if (code.equalsIgnoreCase(ExecutionState.NFC_CMD_EXEC_FAIL.toString())) {
            mNfcCmdExecSate = ExecutionState.NFC_CMD_EXEC_FAIL;
        } else {
            /* Should not be here. */
            logger.debug("Exec state is invalid.");
            mNfcCmdExecSate = ExecutionState.NFC_CMD_EXEC_NA;
        }

        // request command
        code = String.format(Locale.getDefault(), "%d", Integer.parseInt(stateStr.substring(4, 6), 16));
        if (code.equalsIgnoreCase(Command.LOCK.toString())) {
            mCommand = Command.LOCK;
        } else if (code.equalsIgnoreCase(Command.UNLOCK.toString())) {
            mCommand = Command.UNLOCK;
        } else if (code.equalsIgnoreCase(Command.SHOW_KEY.toString())) {
            mCommand = Command.SHOW_KEY;
        } else if (code.equalsIgnoreCase(Command.SIGN.toString())) {
            mCommand = Command.SIGN;
        } else if (code.equalsIgnoreCase(Command.SET_KEY.toString())) {
            mCommand = Command.SET_KEY;
        } else if (code.equalsIgnoreCase(Command.SET_PIN.toString())) {
            mCommand = Command.SET_PIN;
        } else if (code.equalsIgnoreCase(Command.SET_NOTE.toString())) {
            mCommand = Command.SET_NOTE;
        } else if (code.equalsIgnoreCase(Command.CANCEL.toString())) {
            mCommand = Command.CANCEL;
        } else if (code.equalsIgnoreCase(Command.RESET.toString())) {
            mCommand = Command.RESET;
        } else if (code.equalsIgnoreCase(Command.EXPORT_WIF_KEY.toString())) {
            mCommand = Command.EXPORT_WIF_KEY;
        } else {
            /* Should not be here. */
            mCommand = Command.INVALID;
        }

        // Failure reason
        code = stateStr.substring(6, 8);
        if (code.equalsIgnoreCase(FailureReason.NFC_REASON_AUTH_FAILED.getValue())) {
            mFailureReason = FailureReason.NFC_REASON_AUTH_FAILED;
        } else if (code.equalsIgnoreCase((FailureReason.NFC_REASON_CMD_INVALID.getValue()))) {
            mFailureReason = FailureReason.NFC_REASON_CMD_INVALID;
        } else if (code.equalsIgnoreCase((FailureReason.NFC_REASON_PARAM_INVALID.getValue()))) {
            mFailureReason = FailureReason.NFC_REASON_PARAM_INVALID;
        } else if (code.equalsIgnoreCase((FailureReason.NFC_REASON_PARAM_MISSING.getValue()))) {
            mFailureReason = FailureReason.NFC_REASON_PARAM_MISSING;
        } else if (code.equalsIgnoreCase((FailureReason.NFC_REASON_TIMEOUT.getValue()))) {
            mFailureReason = FailureReason.NFC_REASON_TIMEOUT;
        } else if (code.equalsIgnoreCase(FailureReason.NFC_REASON_PIN_UNSET.getValue())) {
            mFailureReason = FailureReason.NFC_REASON_PIN_UNSET;
        } else {
            mFailureReason = FailureReason.NFC_REASON_INVALID;
        }
    }

    private LockState mLockState;
    private ExecutionState mNfcCmdExecSate;
    private Command mCommand;
    private FailureReason mFailureReason;

    public Command getCommand() {
        return mCommand;
    }

    public LockState getLockState() {
        return mLockState;
    }

    public FailureReason getFailureReason() {
        return mFailureReason;
    }

    public ExecutionState getExecutionState() {
        return mNfcCmdExecSate;
    }

    @NotNull
    public String toString() {
        return "\n\tLock state:" + mLockState.name() + "\n\tCommand:" + mCommand.name() + "\n\tExec state:" + mNfcCmdExecSate.name() + "\n\tFailure reason:" + mFailureReason.name();
    }
}
