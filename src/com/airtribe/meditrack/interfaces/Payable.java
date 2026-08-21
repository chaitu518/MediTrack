package com.airtribe.meditrack.interfaces;

import com.airtribe.meditrack.entity.Bill;

public interface Payable {
    Bill generateBill(String billId, double baseAmount, double taxRate);

    default double getTotalAmount(double baseAmount, double taxRate){
        return baseAmount+baseAmount*taxRate;
    }

}