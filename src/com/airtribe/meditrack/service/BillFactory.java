package com.airtribe.meditrack.service;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.interfaces.Payable;

/**
 * Bonus B (Factory pattern): centralizes creation of different Bill
 * "types" (billing strategies) so calling code never does
 * `new Bill(...)` directly and doesn't need to know the tax/discount
 * rules for each strategy.
 */
public class BillFactory {

    public enum BillType {
        STANDARD,   // flat tax rate
        SENIOR_CITIZEN, // extra discount
        INSURANCE   // insurance covers a portion, tax applies to remainder
    }

    /**
     * @param appointment the appointment this bill is for - it's the
     *                     {@link Payable} that generates the STANDARD
     *                     bill directly, and supplies the appointmentId/
     *                     patientId the other two branches need
     * @param doctor       supplies the base consultation fee
     */
    public Bill createBill(BillType type, Appointment appointment, Doctor doctor) {
        String billId = appointment.getId() + "-BILL";
        double baseFee = doctor.getConsultationFee();
        switch (type) {
            case SENIOR_CITIZEN:
                return buildSeniorCitizenBill(billId, appointment, baseFee);
            case INSURANCE:
                return buildInsuranceBill(billId, appointment, baseFee);
            case STANDARD:
            default:
                Payable payable = appointment;
                return payable.generateBill(billId, baseFee, Constants.TAX_RATE);
        }
    }

    private Bill buildSeniorCitizenBill(String billId, Appointment appointment, double baseFee) {
        double discounted = baseFee * 0.9; // 10% discount
        double total = appointment.getTotalAmount(discounted, Constants.TAX_RATE);
        return new Bill(billId, appointment.getId(), appointment.getPatientId(), discounted, Constants.TAX_RATE, total);
    }

    private Bill buildInsuranceBill(String billId, Appointment appointment, double baseFee) {
        double patientShare = baseFee * 0.3; // insurance covers 70%
        double total = appointment.getTotalAmount(patientShare, Constants.TAX_RATE);
        return new Bill(billId, appointment.getId(), appointment.getPatientId(), patientShare, Constants.TAX_RATE, total);
    }
}