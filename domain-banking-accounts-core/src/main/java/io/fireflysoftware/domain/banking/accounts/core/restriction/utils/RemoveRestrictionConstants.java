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


package io.fireflysoftware.domain.banking.accounts.core.restriction.utils;

/**
 * Constants used by {@code RemoveAccountRestrictionSaga}: step identifiers, compensation
 * method names, domain event types, execution-context variable keys, and sentinel step
 * results. Centralizing these here ensures the saga definition, its service entry point,
 * and its tests share a single source of truth for wiring.
 */
public final class RemoveRestrictionConstants {

    private RemoveRestrictionConstants() {}

    // ============================== SAGA NAME ==============================
    public static final String SAGA_REMOVE_RESTRICTION_NAME = "RemoveAccountRestrictionSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_MARK_RESTRICTION_INACTIVE = "markRestrictionInactive";
    public static final String STEP_REVERSE_LEDGER_HOLD = "reverseLedgerHold";

    // ============================== COMPENSATE METHODS ==============================
    public static final String COMPENSATE_REACTIVATE_RESTRICTION = "reactivateRestriction";

    // ============================== EVENT TYPES ==============================
    public static final String EVENT_RESTRICTION_INACTIVATED = "account.restriction.inactivated";
    public static final String EVENT_HOLD_REVERSED = "account.restriction.hold.reversed";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_ACCOUNT_ID = "accountId";
    public static final String CTX_RESTRICTION_ID = "restrictionId";
    public static final String CTX_ORIGINAL_HOLD_TX_ID = "originalHoldTransactionId";
    public static final String CTX_HOLD_AMOUNT = "holdAmount";
    public static final String CTX_ACCOUNT_CURRENCY = "accountCurrency";

    // ============================== SENTINEL RESULTS ==============================
    public static final String RESULT_SKIPPED = "skipped";
}
