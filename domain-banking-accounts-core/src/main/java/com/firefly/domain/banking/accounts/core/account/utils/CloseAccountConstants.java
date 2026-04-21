package com.firefly.domain.banking.accounts.core.account.utils;

public final class CloseAccountConstants {

    private CloseAccountConstants() {}

    // ============================== SAGA NAME ==============================
    public static final String SAGA_CLOSE_ACCOUNT_NAME = "CloseAccountSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_VALIDATE_BALANCE = "validateZeroBalance";
    public static final String STEP_SETTLE = "settleOutstanding";
    public static final String STEP_REMOVE_SPACES = "removeSpaces";
    public static final String STEP_UPDATE_STATUS = "updateAccountStatus";
    public static final String STEP_TERMINATE_CONTRACT = "terminateContract";

    // ============================== COMPENSATE METHODS ==============================
    public static final String COMPENSATE_REVERSE_SETTLEMENT = "reverseSettlement";
    public static final String COMPENSATE_RECREATE_SPACES = "recreateSpaces";
    public static final String COMPENSATE_REVERT_STATUS = "revertAccountStatus";
    public static final String COMPENSATE_REACTIVATE_CONTRACT = "reactivateContract";

    // ============================== EVENT TYPES ==============================
    public static final String EVENT_BALANCE_VALIDATED = "account.balance.validated";
    public static final String EVENT_SETTLED = "account.settled";
    public static final String EVENT_SPACES_REMOVED = "account.spaces.removed";
    public static final String EVENT_STATUS_UPDATED = "account.status.updated";
    public static final String EVENT_CONTRACT_TERMINATED = "contract.terminated";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_ACCOUNT_ID = "accountId";
    public static final String CTX_CONTRACT_ID = "contractId";
    public static final String CTX_SPACES_BACKUP = "spacesBackup";
    public static final String CTX_PREVIOUS_STATUS = "previousStatus";
}
