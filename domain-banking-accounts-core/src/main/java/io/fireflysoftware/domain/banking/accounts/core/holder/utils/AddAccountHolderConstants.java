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


package io.fireflysoftware.domain.banking.accounts.core.holder.utils;

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
