package com.airtribe.meditrack.payment;

import com.airtribe.meditrack.exception.PaymentFailedException;
import com.airtribe.meditrack.interfaces.PaymentStrategy;

/**
 * Concrete Strategy: pays via UPI. Validates the UPI id up front and
 * simulates a gateway charge (no real network call).
 */
public class UpiPayment implements PaymentStrategy {

    private final String upiId;

    public UpiPayment(String upiId) {
        this.upiId = upiId;
    }

    @Override
    public void pay(double amount) throws PaymentFailedException {
        if (upiId == null || !upiId.matches("[\\w.\\-]{2,}@[a-zA-Z]{2,}")) {
            throw new PaymentFailedException("Invalid UPI ID, expected format like name@bank.");
        }
        if (amount <= 0) {
            throw new PaymentFailedException("Payment amount must be positive, got: " + amount);
        }

        System.out.printf("Charged %.2f via UPI (%s).%n", amount, upiId);
    }

    @Override
    public String getPaymentMethod() {
        return "UPI";
    }
}