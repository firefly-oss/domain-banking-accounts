package io.fireflysoftware.domain.banking.accounts.core.account.utils;

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

    // ============================== SUB-TRANSACTION IDS (within STEP_SETTLE) ==============================
    public static final String SETTLE_FEES_TX_KEY = "STEP_SETTLE_FEES";
    public static final String SETTLE_RESIDUAL_TX_KEY = "STEP_SETTLE_RESIDUAL";

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
    public static final String CTX_ACCOUNT_CURRENCY = "accountCurrency";
    public static final String CTX_CONTRACT_ID = "contractId";
    public static final String CTX_SPACES_BACKUP = "spacesBackup";
    public static final String CTX_PREVIOUS_STATUS = "previousStatus";
    public static final String CTX_RESIDUAL_AMOUNT = "residualAmount";
    public static final String CTX_FORCE_CLOSE = "forceClose";
    public static final String CTX_DESTINATION_ACCOUNT_ID = "destinationAccountId";
    public static final String CTX_FEES_OWED = "feesOwed";
    public static final String CTX_SETTLE_FEE_TX_ID = "settleFeeTransactionId";
    public static final String CTX_SETTLE_RESIDUAL_TX_ID = "settleResidualTransactionId";

    // ============================== SENTINEL RESULTS ==============================
    public static final String RESULT_SKIPPED = "skipped";
    public static final String RESULT_SETTLED = "settled";
    public static final String RESULT_BALANCE_VALIDATED = "validated";
}
