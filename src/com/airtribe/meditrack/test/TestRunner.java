package com.airtribe.meditrack.test;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.*;
import com.airtribe.meditrack.exception.AppointmentNotFoundException;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.service.PatientService;
import com.airtribe.meditrack.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Manual test runner (no JUnit): each testXxx() method exercises one slice
 * of behavior with plain assertions, run() catches and reports failures,
 * and main() prints a pass/fail summary at the end.
 */
public class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        run("Doctor CRUD + validation", TestRunner::testDoctorCrudAndValidation);
        run("Patient CRUD + validation", TestRunner::testPatientCrudAndValidation);
        run("Appointment lifecycle + auto-confirm on billing", TestRunner::testAppointmentLifecycleAndBilling);
        run("Billing blocks cancelled appointments", TestRunner::testBillingBlocksCancelledAppointment);
        run("Standard / senior-citizen / insurance billing strategies", TestRunner::testBillingStrategies);
        run("Patient.clone() deep-clones appointment history", TestRunner::testPatientDeepClone);
        run("Appointment.clone() deep-clones notes", TestRunner::testAppointmentDeepClone);
        run("BillSummary is an immutable snapshot", TestRunner::testBillSummaryImmutability);
        run("IdGenerator is a singleton with sequential ids", TestRunner::testIdGeneratorSingleton);

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    private static void testDoctorCrudAndValidation() throws Exception {
        DoctorService ds = new DoctorService();

        Doctor doc = ds.addDoctor("Dr Smith", 45, "9876543210", Specialization.CARDIOLOGY, 500);
        assertTrue(doc.getId() != null && doc.getId().startsWith("DOC-"), "generated doctor id should look like DOC-####");
        assertTrue(ds.searchDoctor(doc.getId()) == doc, "searchDoctor should return the stored instance");

        doc.setConsultationFee(600);
        ds.updateDoctor(doc);
        assertEquals(600.0, ds.searchDoctor(doc.getId()).getConsultationFee(), 0.001, "update should persist the new fee");

        assertTrue(ds.deleteDoctor(doc.getId()), "deleteDoctor should return true for an existing id");
        assertTrue(ds.searchDoctor(doc.getId()) == null, "deleted doctor should no longer be searchable");

        assertThrows(InvalidDataException.class,
                () -> ds.addDoctor("", 45, "9876543210", Specialization.CARDIOLOGY, 500),
                "blank name should be rejected");
        assertThrows(InvalidDataException.class,
                () -> ds.addDoctor("Dr X", -5, "9876543210", Specialization.CARDIOLOGY, 500),
                "invalid age should be rejected");
        assertThrows(InvalidDataException.class,
                () -> ds.addDoctor("Dr X", 45, "123", Specialization.CARDIOLOGY, 500),
                "invalid phone should be rejected");
        assertThrows(InvalidDataException.class,
                () -> ds.addDoctor("Dr X", 45, "9876543210", Specialization.CARDIOLOGY, -10),
                "negative fee should be rejected");
    }

    private static void testPatientCrudAndValidation() throws Exception {
        PatientService ps = new PatientService();

        Patient patient = ps.addPatient("John Doe", 30, "9876543211", "O+", false);
        assertTrue(patient.getId() != null && patient.getId().startsWith("PAT-"), "generated patient id should look like PAT-####");
        assertTrue(ps.searchPatient(patient.getId()) == patient, "searchPatient should return the stored instance");

        patient.setBloodGroup("AB+");
        ps.updatePatient(patient);
        assertEquals("AB+", ps.searchPatient(patient.getId()).getBloodGroup(), "update should persist the new blood group");

        assertTrue(ps.deletePatient(patient.getId()), "deletePatient should return true for an existing id");
        assertTrue(ps.searchPatient(patient.getId()) == null, "deleted patient should no longer be searchable");

        assertThrows(InvalidDataException.class,
                () -> ps.addPatient("   ", 30, "9876543211", "O+", false),
                "blank name should be rejected");
        assertThrows(InvalidDataException.class,
                () -> ps.addPatient("Jane Doe", 200, "9876543211", "O+", false),
                "invalid age should be rejected");
        assertThrows(InvalidDataException.class,
                () -> ps.addPatient("Jane Doe", 30, "not-a-phone", "O+", false),
                "invalid phone should be rejected");
    }

    private static void testAppointmentLifecycleAndBilling() throws Exception {
        DoctorService ds = new DoctorService();
        PatientService ps = new PatientService();
        AppointmentService as = new AppointmentService(ds, ps);

        Doctor doc = ds.addDoctor("Dr Lifecycle", 45, "9876543210", Specialization.NEUROLOGY, 500);
        Patient pat = ps.addPatient("Pat Lifecycle", 30, "9876543211", "O+", false);

        Appointment appt = as.createAppointment(doc.getId(), pat.getId(), LocalDateTime.now().plusDays(1));
        assertEquals(AppointmentStatus.PENDING, appt.getStatus(), "new appointments should start PENDING");

        Bill bill = as.generateBillForAppointment(appt.getId());
        double expectedTotal = 500 * (1 + Constants.TAX_RATE);
        assertEquals(expectedTotal, bill.getTotalAmount(), 0.001, "total should be base amount plus tax");
        assertEquals(AppointmentStatus.CONFIRMED, as.viewAppointment(appt.getId()).getStatus(),
                "a successful bill should auto-confirm a PENDING appointment");

        as.updateStatus(appt.getId(), AppointmentStatus.CANCELLED);
        assertEquals(AppointmentStatus.CANCELLED, as.viewAppointment(appt.getId()).getStatus(),
                "CONFIRMED -> CANCELLED should be an allowed transition");

        assertThrows(InvalidDataException.class,
                () -> as.updateStatus(appt.getId(), AppointmentStatus.CONFIRMED),
                "a cancelled appointment should not be re-confirmable");

        assertThrows(AppointmentNotFoundException.class,
                () -> as.viewAppointment("NO-SUCH-ID"),
                "viewing an unknown appointment id should throw");
    }

    private static void testBillingBlocksCancelledAppointment() throws Exception {
        DoctorService ds = new DoctorService();
        PatientService ps = new PatientService();
        AppointmentService as = new AppointmentService(ds, ps);

        Doctor doc = ds.addDoctor("Dr Block", 45, "9876543210", Specialization.DERMATOLOGY, 400);
        Patient pat = ps.addPatient("Pat Block", 28, "9876543211", "B+", false);
        Appointment appt = as.createAppointment(doc.getId(), pat.getId(), LocalDateTime.now().plusDays(1));

        as.cancelAppointment(appt.getId());

        assertThrows(InvalidDataException.class,
                () -> as.generateBillForAppointment(appt.getId()),
                "a cancelled appointment should not be billable");
    }

    private static void testBillingStrategies() throws Exception {
        DoctorService ds = new DoctorService();
        PatientService ps = new PatientService();
        AppointmentService as = new AppointmentService(ds, ps);

        Doctor doc = ds.addDoctor("Dr Strategy", 50, "9876543210", Specialization.ONCOLOGY, 1000);

        Patient standardPatient = ps.addPatient("Standard Pat", 40, "9876543211", "O+", false);
        Appointment standardAppt = as.createAppointment(doc.getId(), standardPatient.getId(), LocalDateTime.now().plusDays(1));
        Bill standardBill = as.generateBillForAppointment(standardAppt.getId());
        assertEquals(1000.0, standardBill.getBaseAmount(), 0.001, "standard billing should charge the full consultation fee");

        Patient seniorPatient = ps.addPatient("Senior Pat", AppointmentService.SENIOR_CITIZEN_AGE_THRESHOLD, "9876543212", "A+", false);
        Appointment seniorAppt = as.createAppointment(doc.getId(), seniorPatient.getId(), LocalDateTime.now().plusDays(1));
        Bill seniorBill = as.generateBillForAppointment(seniorAppt.getId());
        assertEquals(900.0, seniorBill.getBaseAmount(), 0.001, "senior citizens should get a 10% discount");

        Patient insuredPatient = ps.addPatient("Insured Pat", 35, "9876543213", "B+", true);
        Appointment insuredAppt = as.createAppointment(doc.getId(), insuredPatient.getId(), LocalDateTime.now().plusDays(1));
        Bill insuredBill = as.generateBillForAppointment(insuredAppt.getId());
        assertEquals(300.0, insuredBill.getBaseAmount(), 0.001, "insurance should leave the patient only 30% of the fee");
    }

    private static void testPatientDeepClone() throws Exception {
        PatientService ps = new PatientService();
        AppointmentService as = new AppointmentService(new DoctorService(), ps);

        Patient patient = ps.addPatient("Pat Clone", 30, "9876543211", "O+", false);
        Appointment appt = as.createAppointment("DOC-X", patient.getId(), LocalDateTime.now().plusDays(1));
        patient.addAppointment(appt);

        Patient cloned = patient.clone();

        assertTrue(cloned != patient, "clone() should return a different Patient instance");
        assertTrue(cloned.getAppointmentHistory() != patient.getAppointmentHistory(), "clone should have its own history list");
        assertTrue(cloned.getAppointmentHistory().get(0) != patient.getAppointmentHistory().get(0),
                "clone should deep-clone each Appointment, not share references");
        assertEquals(patient.getAppointmentHistory().get(0).getId(), cloned.getAppointmentHistory().get(0).getId(),
                "cloned appointment should still carry the same data");
    }

    private static void testAppointmentDeepClone() throws Exception {
        Appointment appt = new Appointment("APT-CLONE", "DOC-X", "PAT-X", LocalDateTime.now().plusDays(1));
        appt.addNote("first visit");

        Appointment cloned = appt.clone();
        assertTrue(cloned != appt, "clone() should return a different Appointment instance");
        assertTrue(cloned.getNotes() != appt.getNotes(), "clone should have its own notes list");
        assertEquals(appt.getNotes(), cloned.getNotes(), "cloned notes should start out equal to the original's");

        appt.addNote("second visit");
        assertEquals(1, cloned.getNotes().size(), "mutating the original's notes after cloning should not affect the clone");
    }

    private static void testBillSummaryImmutability() throws Exception {
        Bill bill = new Bill("B-TEST", "APT-TEST", "PAT-TEST", 100, 0.1, 110);
        BillSummary summary = bill.finalizeBill();

        bill.setBaseAmount(999);

        assertEquals(100.0, summary.getBaseAmount(), 0.001,
                "BillSummary should be a frozen snapshot, unaffected by later mutation of the source Bill");
    }

    private static void testIdGeneratorSingleton() throws Exception {
        IdGenerator a = IdGenerator.getInstance();
        IdGenerator b = IdGenerator.getInstance();
        assertTrue(a == b, "IdGenerator.getInstance() should always return the same instance");

        String first = a.nextPatientId();
        String second = a.nextPatientId();
        assertTrue(!first.equals(second), "sequential ids should be distinct");
    }

    // ------------------------------------------------------------------
    // Tiny manual-test harness
    // ------------------------------------------------------------------

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static void run(String name, ThrowingRunnable test) {
        try {
            test.run();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("[FAIL] " + name + " - " + t.getMessage());
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " (expected <" + expected + "> but was <" + actual + ">)");
        }
    }

    private static void assertEquals(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(message + " (expected <" + expected + "> but was <" + actual + ">)");
        }
    }

    private static void assertThrows(Class<? extends Throwable> expectedType, ThrowingRunnable action, String message) {
        try {
            action.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError(message + " (expected " + expectedType.getSimpleName()
                    + " but got " + t.getClass().getSimpleName() + ")");
        }
        throw new AssertionError(message + " (expected " + expectedType.getSimpleName() + " but nothing was thrown)");
    }
}