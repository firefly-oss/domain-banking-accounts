/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.banking.accounts.core.space.utils;

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
    public static final String COMPENSATE_REVERSE_SPACE_INIT = "reverseSpaceInit";
    public static final String COMPENSATE_REMOVE_AUTO_TRANSFER = "removeAutoTransferConfig";

    // ============================== EVENT TYPES ==============================
    public static final String EVENT_SPACE_CREATED = "account.space.created";
    public static final String EVENT_SPACE_INIT_POSTED = "account.space.init.posted";
    public static final String EVENT_AUTO_TRANSFER_CONFIGURED = "account.space.autotransfer.configured";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_ACCOUNT_ID = "accountId";
    public static final String CTX_SPACE_ID = "spaceId";
    public static final String CTX_SOURCE_SPACE_ID = "sourceSpaceId";
    public static final String CTX_ACCOUNT_CURRENCY = "accountCurrency";
    public static final String CTX_INITIAL_AMOUNT = "initialAmount";
    public static final String CTX_SPACE_INIT_TX_ID = "spaceInitTransactionId";

    // ============================== SENTINEL RESULTS ==============================
    public static final String RESULT_SKIPPED = "skipped";
}
