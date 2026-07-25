package com.airtribe.meditrack;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.*;
import com.airtribe.meditrack.exception.AppointmentNotFoundException;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.exception.PaymentFailedException;
import com.airtribe.meditrack.interfaces.ConsoleReminderObserver;
import com.airtribe.meditrack.interfaces.PaymentStrategy;
import com.airtribe.meditrack.payment.CardPayment;
import com.airtribe.meditrack.payment.UpiPayment;
import com.airtribe.meditrack.service.*;
import com.airtribe.meditrack.util.DateUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Menu-driven console entry point. Wires the three services together
 * and dispatches user choices to them.
 *
 * Run with --loadData to load persisted CSV data on startup (Bonus A) -
 * not yet implemented, this is the hook point for it.
 */
public class Main {

    private static final DoctorService doctorService = new DoctorService();
    private static final PatientService patientService = new PatientService();
    private static final AppointmentService appointmentService = new AppointmentService(doctorService, patientService);
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        appointmentService.registerObserver(new ConsoleReminderObserver());

//        boolean loadData = args.length > 0 && Constants.LOAD_DATA_FLAG.equals(args[0]);
//        if (loadData) {
//            System.out.println("(--loadData flag detected - persistence loading is a TODO for Bonus A.)");
//        }

        System.out.println("=== MediTrack ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": addDoctor(); break;
                case "2": addPatient(); break;
                case "3": bookAppointment(); break;
                case "4": cancelAppointment(); break;
                case "5": listDoctors(); break;
                case "6": listPatients(); break;
                case "7": listAppointments(); break;
                case "8": generateBill(); break;
                case "9": updateAppointmentStatus(); break;
                case "10": updateDoctor(); break;
                case "11": deleteDoctor(); break;
                case "12": updatePatient(); break;
                case "13": deletePatient(); break;
                case "0": running = false; break;
                default: System.out.println("Unknown option."); break;
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println("\n1) Add Doctor  2) Add Patient  3) Book Appointment  4) Cancel Appointment");
        System.out.println("5) List Doctors  6) List Patients  7) List Appointments  8) Generate Bill");
        System.out.println("9) Update Appointment Status  10) Update Doctor  11) Delete Doctor");
        System.out.println("12) Update Patient  13) Delete Patient  0) Exit");
        System.out.print("> ");
    }

    private static void addDoctor() {
        try {
            System.out.print("Name: ");
            String name = scanner.nextLine();
            System.out.print("Age: ");
            int age = Integer.parseInt(scanner.nextLine());
            System.out.print("Phone (10 digits): ");
            String phone = scanner.nextLine();
            System.out.print("Specialization " + java.util.Arrays.toString(Specialization.values()) + ": ");
            Specialization spec = Specialization.valueOf(scanner.nextLine().trim().toUpperCase());
            System.out.print("Consultation fee: ");
            double fee = Double.parseDouble(scanner.nextLine());

            Doctor doc = doctorService.addDoctor(name, age, phone, spec, fee);
            System.out.println("Added: " + doc.summary());
        } catch (InvalidDataException | IllegalArgumentException e) {
            System.out.println("Could not add doctor: " + e.getMessage());
        }
    }

    private static void addPatient() {
        try {
            System.out.print("Name: ");
            String name = scanner.nextLine();
            System.out.print("Age: ");
            int age = Integer.parseInt(scanner.nextLine());
            System.out.print("Phone (10 digits): ");
            String phone = scanner.nextLine();
            System.out.print("Blood group: ");
            String bloodGroup = scanner.nextLine();
            System.out.print("Has insurance? (y/n): ");
            boolean hasInsurance = scanner.nextLine().trim().equalsIgnoreCase("y");

            Patient patient = patientService.addPatient(name, age, phone, bloodGroup, hasInsurance);
            System.out.println("Added: " + patient.summary());
        } catch (InvalidDataException | NumberFormatException e) {
            System.out.println("Could not add patient: " + e.getMessage());
        }
    }

    private static void updateDoctor() {
        System.out.print("Doctor ID: ");
        String id = scanner.nextLine();
        Doctor doctor = doctorService.searchDoctor(id);
        if (doctor == null) {
            System.out.println("No such doctor.");
            return;
        }

        System.out.print("Name [" + doctor.getName() + "]: ");
        String name = scanner.nextLine();
        System.out.print("Age [" + doctor.getAge() + "]: ");
        String ageInput = scanner.nextLine();
        System.out.print("Phone (10 digits) [" + doctor.getPhoneNumber() + "]: ");
        String phone = scanner.nextLine();
        System.out.print("Specialization " + java.util.Arrays.toString(Specialization.values()) + " [" + doctor.getSpecialization() + "]: ");
        String specInput = scanner.nextLine();
        System.out.print("Consultation fee [" + doctor.getConsultationFee() + "]: ");
        String feeInput = scanner.nextLine();

        try {
            // Parse every changed field up front so a bad number/enum doesn't
            // leave the doctor half-updated after earlier setters already ran.
            Integer age = ageInput.isBlank() ? null : Integer.parseInt(ageInput);
            Double fee = feeInput.isBlank() ? null : Double.parseDouble(feeInput);
            Specialization spec = specInput.isBlank() ? null : Specialization.valueOf(specInput.trim().toUpperCase());

            if (!name.isBlank()) doctor.setName(name);
            if (age != null) doctor.setAge(age);
            if (!phone.isBlank()) doctor.setPhoneNumber(phone);
            if (spec != null) doctor.setSpecialization(spec);
            if (fee != null) doctor.setConsultationFee(fee);

            doctorService.updateDoctor(doctor);
            System.out.println("Updated: " + doctor.summary());
        } catch (InvalidDataException | IllegalArgumentException e) {
            System.out.println("Could not update doctor: " + e.getMessage());
        }
    }

    private static void deleteDoctor() {
        System.out.print("Doctor ID: ");
        String id = scanner.nextLine();
        if (doctorService.deleteDoctor(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("No such doctor.");
        }
    }

    private static void updatePatient() {
        System.out.print("Patient ID: ");
        String id = scanner.nextLine();
        Patient patient = patientService.searchPatient(id);
        if (patient == null) {
            System.out.println("No such patient.");
            return;
        }

        System.out.print("Name [" + patient.getName() + "]: ");
        String name = scanner.nextLine();
        System.out.print("Age [" + patient.getAge() + "]: ");
        String ageInput = scanner.nextLine();
        System.out.print("Phone (10 digits) [" + patient.getPhoneNumber() + "]: ");
        String phone = scanner.nextLine();
        System.out.print("Blood group [" + patient.getBloodGroup() + "]: ");
        String bloodGroup = scanner.nextLine();
        System.out.print("Has insurance? (y/n) [" + (patient.hasInsurance() ? "y" : "n") + "]: ");
        String insuranceInput = scanner.nextLine();

        try {
            Integer age = ageInput.isBlank() ? null : Integer.parseInt(ageInput);

            if (!name.isBlank()) patient.setName(name);
            if (age != null) patient.setAge(age);
            if (!phone.isBlank()) patient.setPhoneNumber(phone);
            if (!bloodGroup.isBlank()) patient.setBloodGroup(bloodGroup);
            if (!insuranceInput.isBlank()) patient.setHasInsurance(insuranceInput.trim().equalsIgnoreCase("y"));

            patientService.updatePatient(patient);
            System.out.println("Updated: " + patient.summary());
        } catch (InvalidDataException | IllegalArgumentException e) {
            System.out.println("Could not update patient: " + e.getMessage());
        }
    }

    private static void deletePatient() {
        System.out.print("Patient ID: ");
        String id = scanner.nextLine();
        if (patientService.deletePatient(id)) {
            System.out.println("Deleted.");
        } else {
            System.out.println("No such patient.");
        }
    }

    private static void bookAppointment() {
        System.out.print("Doctor ID: ");
        String docId = scanner.nextLine();
        System.out.print("Patient ID: ");
        String patId = scanner.nextLine();
        System.out.print("Date/time (yyyy-MM-dd HH:mm, e.g. " + DateUtil.format(LocalDateTime.now().plusDays(1)) + "): ");
        String dateTimeInput = scanner.nextLine();

        if (doctorService.searchDoctor(docId) == null) {
            System.out.println("No such doctor.");
            return;
        }
        Patient patient = patientService.searchPatient(patId);
        if (patient == null) {
            System.out.println("No such patient.");
            return;
        }

        LocalDateTime dateTime;
        try {
            dateTime = DateUtil.parse(dateTimeInput.trim());
        } catch (DateTimeParseException e) {
            System.out.println("Could not book appointment: invalid date/time format.");
            return;
        }
        if (!DateUtil.isFuture(dateTime)) {
            System.out.println("Could not book appointment: date/time must be in the future.");
            return;
        }

        try {
            Appointment appt = appointmentService.createAppointment(docId, patId, dateTime);
            patient.addAppointment(appt);
            System.out.println("Booked: " + appt.summary());
        } catch (InvalidDataException e) {
            System.out.println("Could not book appointment: " + e.getMessage());
        }
    }

    private static void cancelAppointment() {
        System.out.print("Appointment ID: ");
        String id = scanner.nextLine();
        try {
            appointmentService.cancelAppointment(id);
            System.out.println("Cancelled.");
        } catch (AppointmentNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void updateAppointmentStatus() {
        System.out.print("Appointment ID: ");
        String id = scanner.nextLine();
        System.out.print("New status " + java.util.Arrays.toString(AppointmentStatus.values()) + ": ");
        try {
            AppointmentStatus newStatus = AppointmentStatus.valueOf(scanner.nextLine().trim().toUpperCase());
            appointmentService.updateStatus(id, newStatus);
            System.out.println("Updated to " + newStatus + ".");
        } catch (AppointmentNotFoundException | InvalidDataException | IllegalArgumentException e) {
            System.out.println("Could not update status: " + e.getMessage());
        }
    }

    private static void listDoctors() {
        List<Doctor> doctors = doctorService.getAllDoctors();
        if (doctors.isEmpty()) {
            System.out.println("(no doctors yet)");
        }
        doctors.forEach(d -> System.out.println(d.summary()));
    }

    private static void listPatients() {
        List<Patient> patients = patientService.getAllPatients();
        if (patients.isEmpty()) {
            System.out.println("(no patients yet)");
        }
        patients.forEach(p -> System.out.println(p.summary()));
    }

    private static void listAppointments() {
        List<Appointment> appts = appointmentService.getAllAppointments();
        if (appts.isEmpty()) {
            System.out.println("(no appointments yet)");
        }
        appts.forEach(a -> System.out.println(a.summary()));
    }

    private static void generateBill() {
        System.out.print("Appointment ID: ");
        String appointmentId = scanner.nextLine();

        PaymentStrategy paymentStrategy = choosePaymentStrategy();
        if (paymentStrategy == null) {
            System.out.println("No bill generated: a valid payment method is required.");
            return;
        }

        try {
            Bill bill = appointmentService.generateBillForAppointment(appointmentId, paymentStrategy);
            BillSummary summary = bill.finalizeBill();
            System.out.println("Payment successful via " + paymentStrategy.getPaymentMethod() + ".");
            System.out.println(summary);
        } catch (AppointmentNotFoundException | InvalidDataException | PaymentFailedException e) {
            System.out.println("Could not generate bill: " + e.getMessage());
        }
    }

    /**
     * Strategy pattern entry point: lets the user pick how they want to
     * pay (card or UPI) and collects the details that strategy needs.
     * Returns null on an invalid choice so the caller can bail out
     * without ever touching the appointment/bill.
     */
    private static PaymentStrategy choosePaymentStrategy() {
        System.out.print("Payment method - 1) Card  2) UPI: ");
        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1": {
                System.out.print("Card number (16 digits): ");
                String cardNumber = scanner.nextLine();
                System.out.print("Card holder name: ");
                String holderName = scanner.nextLine();
                System.out.print("Expiry (MM/YY): ");
                String expiry = scanner.nextLine();
                System.out.print("CVV: ");
                String cvv = scanner.nextLine();
                return new CardPayment(cardNumber, holderName, expiry, cvv);
            }
            case "2": {
                System.out.print("UPI ID (e.g. name@bank): ");
                String upiId = scanner.nextLine();
                return new UpiPayment(upiId);
            }
            default:
                System.out.println("Unknown payment method.");
                return null;
        }
    }
}