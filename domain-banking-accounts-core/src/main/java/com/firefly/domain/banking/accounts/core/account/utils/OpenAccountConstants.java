package com.firefly.domain.banking.accounts.core.account.utils;

public final class OpenAccountConstants {

    private OpenAccountConstants() {}

    // ============================== SAGA NAME ==============================
    public static final String SAGA_OPEN_ACCOUNT_NAME = "OpenAccountSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_CREATE_CONTRACT = "createContract";
    public static final String STEP_ADD_PARTY = "addPartyToContract";
    public static final String STEP_CREATE_ACCOUNT = "createAccount";
    public static final String STEP_INIT_BALANCE = "initializeBalance";
    public static final String STEP_CREATE_SPACE = "createMainSpace";
    public static final String STEP_RECORD_STATUS = "recordStatusHistory";
    public static final String STEP_SET_PARAMS = "setDefaultParameters";

    // ============================== COMPENSATE METHODS ==============================
    public static final String COMPENSATE_TERMINATE_CONTRACT = "terminateContract";
    public static final String COMPENSATE_REMOVE_PARTY = "removePartyFromContract";
    public static final String COMPENSATE_CLOSE_ACCOUNT = "closeAccount";
    public static final String COMPENSATE_DELETE_BALANCE = "deleteBalance";
    public static final String COMPENSATE_DELETE_SPACE = "deleteSpace";
    public static final String COMPENSATE_DELETE_PARAMS = "deleteParameters";

    // ============================== EVENT TYPES ==============================
    public static final String EVENT_CONTRACT_CREATED = "contract.created";
    public static final String EVENT_PARTY_ADDED = "contract.party.added";
    public static final String EVENT_ACCOUNT_CREATED = "account.created";
    public static final String EVENT_BALANCE_INITIALIZED = "account.balance.initialized";
    public static final String EVENT_SPACE_CREATED = "account.space.created";
    public static final String EVENT_STATUS_RECORDED = "account.status.recorded";
    public static final String EVENT_PARAMS_SET = "account.params.set";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_CONTRACT_ID = "contractId";
    public static final String CTX_ACCOUNT_ID = "accountId";
    public static final String CTX_PARTY_ID = "partyId";
    public static final String CTX_BALANCE_ID = "balanceId";
    public static final String CTX_MAIN_SPACE_ID = "mainSpaceId";
    public static final String CTX_STATUS_ENTRY_ID = "statusEntryId";
}
