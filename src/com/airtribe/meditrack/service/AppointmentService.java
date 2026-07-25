package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.exception.AppointmentNotFoundException;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Create/view/cancel appointments, and - since payment is what actually
 * confirms a booking - bill generation too. Fires AppointmentObserver
 * callbacks on create/cancel (Observer pattern) so other parts of the
 * app (or mentee-added notifiers) can react without AppointmentService
 * knowing about them.
 */
public class AppointmentService {

    public static final int SENIOR_CITIZEN_AGE_THRESHOLD = 60;

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> VALID_TRANSITIONS = new EnumMap<>(AppointmentStatus.class);
    static {
        VALID_TRANSITIONS.put(AppointmentStatus.PENDING, EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED));
        VALID_TRANSITIONS.put(AppointmentStatus.CONFIRMED, EnumSet.of(AppointmentStatus.CANCELLED));
        VALID_TRANSITIONS.put(AppointmentStatus.CANCELLED, EnumSet.noneOf(AppointmentStatus.class));
    }


    private final DataStore<Appointment> store = new DataStore<>(Appointment::getId);
    private final List<AppointmentObserver> observers = new ArrayList<>();
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final BillFactory billFactory;

    public AppointmentService(DoctorService doctorService, PatientService patientService) {
        this.doctorService = doctorService;
        this.patientService = patientService;
        this.billFactory = new BillFactory();
    }

    public void registerObserver(AppointmentObserver observer) {
        observers.add(observer);
    }

    public Appointment createAppointment(String doctorId, String patientId, LocalDateTime dateTime) {
        String id = IdGenerator.getInstance().nextAppointmentId();
        Appointment appointment = new Appointment(id, doctorId, patientId, dateTime);
        store.save(appointment);
        notifyCreated(appointment);
        return appointment;
    }

    public Appointment viewAppointment(String id) throws AppointmentNotFoundException {
        Appointment appt = store.findById(id);
        if (appt == null) {
            throw new AppointmentNotFoundException("No appointment found with ID: " + id);
        }
        return appt;
    }

    public void cancelAppointment(String id) throws AppointmentNotFoundException {
        Appointment appt = viewAppointment(id); // throws if missing
        appt.setStatus(AppointmentStatus.CANCELLED);
        notifyCancelled(appt);
    }

    public void confirmAppointment(String id) throws AppointmentNotFoundException {
        Appointment appt = viewAppointment(id);
        appt.setStatus(AppointmentStatus.CONFIRMED);
    }

    public List<Appointment> getAllAppointments() {
        return store.findAll();
    }

    public List<Appointment> getAppointmentsForPatient(String patientId) {
        return store.findAll().stream()
                .filter(a -> a.getPatientId().equals(patientId))
                .collect(Collectors.toList());
    }

    public List<Appointment> getAppointmentsForDoctor(String doctorId) {
        return store.findAll().stream()
                .filter(a -> a.getDoctorId().equals(doctorId))
                .collect(Collectors.toList());
    }

    public void updateStatus(String id, AppointmentStatus newStatus)
            throws AppointmentNotFoundException, InvalidDataException {
        Appointment appt = viewAppointment(id);
        AppointmentStatus current = appt.getStatus();

        Set<AppointmentStatus> allowed = VALID_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new InvalidDataException(
                    "Cannot move appointment " + id + " from " + current + " to " + newStatus
                            + " (allowed from " + current + ": " + allowed + ")");
        }
        appt.setStatus(newStatus);
        if (newStatus == AppointmentStatus.CANCELLED) {
            notifyCancelled(appt);
        }
    }

    /**
     * Generates a real, data-driven Bill for a given appointment: base
     * amount = that appointment's doctor's consultation fee; bill type =
     * derived from the patient's age/insurance, not chosen by the caller.
     * A successful bill counts as payment, so a PENDING appointment is
     * moved to CONFIRMED; a CANCELLED appointment can't be billed at all.
     */
    public Bill generateBillForAppointment(String appointmentId)
            throws AppointmentNotFoundException, InvalidDataException {

        Appointment appointment = viewAppointment(appointmentId);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidDataException("Cannot generate a bill for a cancelled appointment: " + appointmentId);
        }

        Doctor doctor = doctorService.searchDoctor(appointment.getDoctorId());
        if (doctor == null) {
            throw new InvalidDataException("Appointment " + appointmentId
                    + " references a doctor that no longer exists: " + appointment.getDoctorId());
        }

        Patient patient = patientService.searchPatient(appointment.getPatientId());
        if (patient == null) {
            throw new InvalidDataException("Appointment " + appointmentId
                    + " references a patient that no longer exists: " + appointment.getPatientId());
        }

        BillFactory.BillType type = resolveBillType(patient);
        Bill bill = billFactory.createBill(type, appointment, doctor);

        if (appointment.getStatus() == AppointmentStatus.PENDING) {
            updateStatus(appointmentId, AppointmentStatus.CONFIRMED);
        }

        return bill;
    }

    private BillFactory.BillType resolveBillType(Patient patient) {
        if (patient.getAge() >= SENIOR_CITIZEN_AGE_THRESHOLD) {
            return BillFactory.BillType.SENIOR_CITIZEN;
        }
        if (patient.hasInsurance()) {
            return BillFactory.BillType.INSURANCE;
        }
        return BillFactory.BillType.STANDARD;
    }

    public long countAppointmentsForDoctor(String doctorId) {
        return store.findAll().stream()
                .filter(a -> a.getDoctorId().equals(doctorId))
                .count();
    }

    private void notifyCreated(Appointment appointment) {
        for (AppointmentObserver obs : observers) {
            obs.onAppointmentCreated(appointment);
        }
    }

    private void notifyCancelled(Appointment appointment) {
        for (AppointmentObserver obs : observers) {
            obs.onAppointmentCancelled(appointment);
        }
    }
}