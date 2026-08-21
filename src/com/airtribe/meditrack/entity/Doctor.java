package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.util.Validator;

public class Doctor extends Person {
    private Specialization specialization;
    private double consultationFee;

    public Doctor(String id, String name, int age, String phoneNumber, Specialization specialization,double consultationFee) {
        super(id, name, age, phoneNumber);
        this.specialization = specialization;
        this.consultationFee = consultationFee;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public double getConsultationFee() {
        return consultationFee;
    }
    public void setConsultationFee(double consultationFee) throws InvalidDataException {
        Validator.validateFee(consultationFee);
        this.consultationFee = consultationFee;
    }

    @Override
    public String summary() {
        return String.format("%s | %s | Fee: %.2f",
                super.summary(), specialization, consultationFee);
    }
}