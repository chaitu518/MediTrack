package com.airtribe.meditrack.interfaces;

import com.airtribe.meditrack.exception.PaymentFailedException;

/**
 * Strategy pattern: each payment method (card, UPI, ...) knows how to
 * validate its own details and process a charge. AppointmentService
 * depends only on this interface, so a bill is generated - and the
 * appointment confirmed - only after {@link #pay} completes without
 * throwing.
 */
public interface PaymentStrategy {

    void pay(double amount) throws PaymentFailedException;

    String getPaymentMethod();
}