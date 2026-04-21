package com.firefly.domain.banking.accounts.core.restriction.utils;

public final class ApplyRestrictionConstants {

    private ApplyRestrictionConstants() {}

    // ============================== SAGA NAME ==============================
    public static final String SAGA_APPLY_RESTRICTION_NAME = "ApplyAccountRestrictionSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_CREATE_RESTRICTION = "createRestriction";
    public static final String STEP_UPDATE_STATUS = "updateAccountStatus";
    public static final String STEP_NOTIFY = "notifyHolder";

    // ============================== COMPENSATE METHODS ==============================
    public static final String COMPENSATE_REMOVE_RESTRICTION = "removeRestriction";
    public static final String COMPENSATE_REVERT_STATUS = "revertAccountStatus";

    // ============================== EVENT TYPES ==============================
    public static final String EVENT_RESTRICTION_CREATED = "account.restriction.created";
    public static final String EVENT_STATUS_UPDATED = "account.status.updated";
    public static final String EVENT_HOLDER_NOTIFIED = "account.holder.notified";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_RESTRICTION_ID = "restrictionId";
    public static final String CTX_ACCOUNT_ID = "accountId";
    public static final String CTX_PREVIOUS_STATUS = "previousStatus";
}
