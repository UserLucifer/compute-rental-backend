package com.compute.rental.common.enums;

public enum RentalOrderStatus {
    /**
     * Order created, machine fee not paid.
     */
    PENDING_PAY,

    /**
     * Reserved status.
     * Current flow goes directly from PENDING_PAY to PENDING_ACTIVATION after machine fee payment,
     * because API credential generation happens in the same transaction.
     * Do not expose this as a selectable/front-end filter status unless the state machine is changed.
     */
    PAID,

    /**
     * Machine fee paid and API credential generated, waiting for deploy fee payment.
     */
    PENDING_ACTIVATION,

    /**
     * Legacy short-lived status. Deploy fee payment now starts the rental immediately.
     */
    ACTIVATING,

    /**
     * Rental was auto-paused after deployment; user can start it again to resume profit generation.
     */
    PAUSED,

    /**
     * Rental is running and daily profit generation is enabled.
     */
    RUNNING,

    /**
     * Rental reached profit_end_at and expiration settlement finished.
     */
    EXPIRED,

    /**
     * Settlement is being processed. Used as a short-lived lock status.
     */
    SETTLING,

    /**
     * Reserved status.
     * Current flow uses settlement_status=SETTLED to represent settlement completion.
     * Final order_status is EXPIRED for normal expiry or EARLY_CLOSED for early settlement.
     * Do not expose this as a selectable/front-end filter status unless the state machine is changed.
     */
    SETTLED,

    /**
     * Early settlement finished.
     */
    EARLY_CLOSED,

    /**
     * Order canceled before activation or due to activation timeout.
     */
    CANCELED
}
