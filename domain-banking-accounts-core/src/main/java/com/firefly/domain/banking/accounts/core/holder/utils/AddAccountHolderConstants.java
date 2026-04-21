package com.firefly.domain.banking.accounts.core.holder.utils;

public final class AddAccountHolderConstants {

    private AddAccountHolderConstants() {}

    // ============================== SAGA NAME ==============================
    public static final String SAGA_ADD_ACCOUNT_HOLDER_NAME = "AddAccountHolderSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_GET_CONTRACT = "getAccountContract";
    public static final String STEP_ADD_PARTY = "addPartyToContract";
    public static final String STEP_RECORD_ACTIVITY = "recordActivity";

    // ============================== COMPENSATE METHODS ==============================
    public static final String COMPENSATE_REMOVE_PARTY = "removePartyFromContract";

    // ============================== EVENT TYPES ==============================
    public static final String EVENT_CONTRACT_RETRIEVED = "contract.retrieved";
    public static final String EVENT_PARTY_ADDED = "contract.party.added";
    public static final String EVENT_ACTIVITY_RECORDED = "account.activity.recorded";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_CONTRACT_ID = "contractId";
    public static final String CTX_ACCOUNT_ID = "accountId";
    public static final String CTX_PARTY_ID = "partyId";
}
