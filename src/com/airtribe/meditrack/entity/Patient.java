package com.airtribe.meditrack.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Patient extends Person implements Cloneable {
    private String bloodGroup;
    private boolean hasInsurance;
    private List<Appointment> appointmentHistory;

    public Patient(String id, String name, int age, String phoneNumber, String bloodGroup, boolean hasInsurance) {
        super(id, name, age, phoneNumber);
        this.bloodGroup = bloodGroup;
        this.hasInsurance = hasInsurance;
        this.appointmentHistory = new ArrayList<>();
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public boolean hasInsurance() {
        return hasInsurance;
    }

    public void setHasInsurance(boolean hasInsurance) {
        this.hasInsurance = hasInsurance;
    }
    public List<Appointment> getAppointmentHistory() {
        return appointmentHistory;
    }

    public void addAppointment(Appointment appointment) {
        this.appointmentHistory.add(appointment);
    }

    @Override
    public Patient clone() {
        Patient clonedPatient = new Patient(this.getId(), this.getName(), this.getAge(), this.getPhoneNumber(),
                this.bloodGroup, this.hasInsurance);
        clonedPatient.appointmentHistory = this.appointmentHistory.stream()
                .map(Appointment::clone)
                .collect(Collectors.toCollection(ArrayList::new));
        return clonedPatient;
    }

    @Override
    public String summary() {
        return String.format("%s | Blood Group: %s | Insurance: %s | %d appointment(s)",
                super.summary(), bloodGroup, hasInsurance ? "Yes" : "No", appointmentHistory.size());
    }
}