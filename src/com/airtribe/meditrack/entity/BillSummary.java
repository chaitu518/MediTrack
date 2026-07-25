package com.airtribe.meditrack.entity;

public final class BillSummary {
    private final String billId;
    private final String appointmentId;
    private final String patientId;
    private final double baseAmount;
    private final double taxRate;
    private final double totalAmount;

    public BillSummary(String billId, String appointmentId, String patientId, double baseAmount, double taxRate, double totalAmount) {
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
    public double getTaxRate() {
        return taxRate;
    }
    public double getTotalAmount() {
        return totalAmount;
    }
    @Override
    public String toString() {
        return "BillSummary{" +
                "billId='" + billId + '\'' +
                ", appointmentId='" + appointmentId + '\'' +
                ", patientId='" + patientId + '\'' +
                ", baseAmount=" + baseAmount +
                ", taxRate=" + taxRate +
                ", totalAmount=" + totalAmount +
                '}';
    }

}
