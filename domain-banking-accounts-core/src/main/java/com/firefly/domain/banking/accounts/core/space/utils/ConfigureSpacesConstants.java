package com.firefly.domain.banking.accounts.core.space.utils;

public final class ConfigureSpacesConstants {

    private ConfigureSpacesConstants() {}

    // ============================== SAGA NAME ==============================
    public static final String SAGA_CONFIGURE_SPACES_NAME = "ConfigureAccountSpacesSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_CREATE_SPACE = "createSpace";
    public static final String STEP_INIT_SPACE_BALANCE = "initializeSpaceBalance";
    public static final String STEP_CONFIG_AUTO_TRANSFER = "configureAutoTransfer";

    // ============================== COMPENSATE METHODS ==============================
    public static final String COMPENSATE_DELETE_SPACE = "deleteSpace";
    public static final String COMPENSATE_DELETE_BALANCE = "deleteBalance";
    public static final String COMPENSATE_REMOVE_AUTO_TRANSFER = "removeAutoTransferConfig";

    // ============================== EVENT TYPES ==============================
    public static final String EVENT_SPACE_CREATED = "account.space.created";
    public static final String EVENT_BALANCE_INITIALIZED = "account.space.balance.initialized";
    public static final String EVENT_AUTO_TRANSFER_CONFIGURED = "account.space.autotransfer.configured";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_SPACE_ID = "spaceId";
    public static final String CTX_BALANCE_ID = "balanceId";
    public static final String CTX_ACCOUNT_ID = "accountId";
}
