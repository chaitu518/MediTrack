package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.util.Validator;

public class Bill {

    private final String billId;
    private final String appointmentId;
    private final String patientId;
    private double baseAmount;
    private double taxRate;
    private double totalAmount;

    public Bill(String billId, String appointmentId, String patientId,
                double baseAmount, double taxRate, double totalAmount) {
        this.billId = billId;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.baseAmount = baseAmount;
        this.taxRate = taxRate;
        this.totalAmount = totalAmount;
    }

    public String getBillId() {
        return billId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public double getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(double baseAmount) throws InvalidDataException {
        Validator.validateFee(baseAmount);
        this.baseAmount = baseAmount;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) throws InvalidDataException {
        Validator.validateFee(totalAmount);
        this.totalAmount = totalAmount;
    }

    /**
     * Snapshot this mutable Bill into an immutable BillSummary -
     * once created, the summary can never be altered again.
     */
    public BillSummary finalizeBill() {
        return new BillSummary(billId, appointmentId, patientId, baseAmount, taxRate, totalAmount);
    }

    @Override
    public String toString() {
        return String.format("Bill[%s] appointment=%s patient=%s base=%.2f tax=%.2f total=%.2f",
                billId, appointmentId, patientId, baseAmount, taxRate, totalAmount);
    }
}
