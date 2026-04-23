package com.firefly.domain.banking.accounts.core.restriction.utils;

import java.util.Set;

public final class ApplyRestrictionConstants {

    private ApplyRestrictionConstants() {}

    // ============================== SAGA NAME ==============================
    public static final String SAGA_APPLY_RESTRICTION_NAME = "ApplyAccountRestrictionSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_CREATE_RESTRICTION = "createRestriction";
    public static final String STEP_POST_LEDGER_HOLD = "postLedgerHold";
    public static final String STEP_UPDATE_STATUS = "updateAccountStatus";
    public static final String STEP_NOTIFY = "notifyHolder";

    // ============================== COMPENSATE METHODS ==============================
    public static final String COMPENSATE_REMOVE_RESTRICTION = "removeRestriction";
    public static final String COMPENSATE_REVERSE_HOLD = "reverseLedgerHold";
    public static final String COMPENSATE_REVERT_STATUS = "revertAccountStatus";

    // ============================== EVENT TYPES ==============================
    public static final String EVENT_RESTRICTION_CREATED = "account.restriction.created";
    public static final String EVENT_HOLD_POSTED = "account.restriction.hold.posted";
    public static final String EVENT_STATUS_UPDATED = "account.status.updated";
    public static final String EVENT_HOLDER_NOTIFIED = "account.holder.notified";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_ACCOUNT_ID = "accountId";
    public static final String CTX_RESTRICTION_ID = "restrictionId";
    public static final String CTX_RESTRICTION_TYPE = "restrictionType";
    public static final String CTX_RESTRICTED_AMOUNT = "restrictedAmount";
    public static final String CTX_ACCOUNT_CURRENCY = "accountCurrency";
    public static final String CTX_HOLD_TX_ID = "holdTransactionId";
    public static final String CTX_PREVIOUS_STATUS = "previousStatus";

    // ============================== SENTINEL RESULTS ==============================
    public static final String RESULT_SKIPPED = "skipped";

    // ============================== MONETARY RESTRICTION TYPES ==============================
    /**
     * Restriction types whose {@code restrictedAmount} reserves money in the ledger (DEBIT customer,
     * CREDIT holds-clearing GL). Non-monetary restrictions (dormancy, sanctions screening, limits
     * that don't bind an amount) skip the ledger-hold step.
     */
    public static final Set<String> MONETARY_RESTRICTIONS = Set.of(
            "WITHDRAWAL_HOLD",
            "LEGAL_ORDER",
            "COURT_ORDER",
            "GARNISHMENT",
            "FRAUD_INVESTIGATION"
    );
}
