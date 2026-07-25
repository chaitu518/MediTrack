package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.interfaces.Payable;
import com.airtribe.meditrack.util.DateUtil;
import com.airtribe.meditrack.util.Validator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Appointment extends MedicalEntity implements Cloneable, Payable {
    private String doctorId;
    private String patientId;
    private AppointmentStatus status;
    private final List<String> notes;
    private LocalDateTime dateTime;

    public Appointment(String id, String doctorId, String patientId, LocalDateTime dateTime) {
        super(id);
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.dateTime = dateTime;
        this.status = AppointmentStatus.PENDING;
        this.notes = new ArrayList<>();
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) throws InvalidDataException {
        Validator.validateId(doctorId);
        this.doctorId = doctorId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) throws InvalidDataException {
        Validator.validateId(patientId);
        this.patientId = patientId;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void addNote(String note) {
        notes.add(note);
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public Appointment clone() {
        Appointment clonedAppointment = new Appointment(this.getId(), this.doctorId, this.patientId, this.dateTime);
        clonedAppointment.setStatus(this.status);
        clonedAppointment.notes.addAll(this.notes);
        return clonedAppointment;
    }

    @Override
    public String summary() {
        return String.format("[%s] Appointment with Dr. %s for Patient %s on %s (Status: %s)", getId(), doctorId, patientId, DateUtil.format(dateTime), status);
    }

    /**
     * This appointment's own standard-rate bill: it already knows its own
     * id and patientId, so it just needs the doctor's fee and the tax
     * rate from the caller. Senior-citizen/insurance adjustments are
     * applied by BillFactory on top of a discounted baseAmount instead.
     */
    @Override
    public Bill generateBill(String billId, double baseAmount, double taxRate) {
        double total = getTotalAmount(baseAmount, taxRate);
        return new Bill(billId, this.getId(), this.patientId, baseAmount, taxRate, total);
    }
}
