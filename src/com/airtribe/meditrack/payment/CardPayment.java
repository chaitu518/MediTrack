package com.airtribe.meditrack.payment;

import com.airtribe.meditrack.exception.PaymentFailedException;
import com.airtribe.meditrack.interfaces.PaymentStrategy;

/**
 * Concrete Strategy: pays by card. Validates card details up front and
 * simulates a gateway charge (no real network call).
 */
public class CardPayment implements PaymentStrategy {

    private final String cardNumber;
    private final String cardHolderName;
    private final String expiry; // MM/YY
    private final String cvv;

    public CardPayment(String cardNumber, String cardHolderName, String expiry, String cvv) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.expiry = expiry;
        this.cvv = cvv;
    }

    @Override
    public void pay(double amount) throws PaymentFailedException {
        if (cardNumber == null || !cardNumber.replaceAll("\\s", "").matches("\\d{16}")) {
            throw new PaymentFailedException("Invalid card number: must be 16 digits.");
        }
        if (cardHolderName == null || cardHolderName.trim().isEmpty()) {
            throw new PaymentFailedException("Card holder name must not be blank.");
        }
        if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new PaymentFailedException("Invalid expiry, expected MM/YY.");
        }
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new PaymentFailedException("Invalid CVV.");
        }
        if (amount <= 0) {
            throw new PaymentFailedException("Payment amount must be positive, got: " + amount);
        }

        System.out.printf("Charged %.2f to card ending %s.%n", amount, cardNumber.substring(cardNumber.length() - 4));
    }

    @Override
    public String getPaymentMethod() {
        return "CARD";
    }
}