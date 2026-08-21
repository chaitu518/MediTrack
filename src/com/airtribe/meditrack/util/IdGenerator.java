package com.airtribe.meditrack.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton (LAZY initialization, double-checked locking) that hands out
 * sequential, prefixed IDs (e.g. "DOC-0001"). Lazy because the instance
 * is only created on first use - contrast with AppConfig, which uses
 * EAGER initialization.
 */
public final class IdGenerator {

    // volatile is required for double-checked locking to be safe under the JMM.
    private static volatile IdGenerator instance;

    private final AtomicInteger doctorSeq = new AtomicInteger(0);
    private final AtomicInteger patientSeq = new AtomicInteger(0);
    private final AtomicInteger appointmentSeq = new AtomicInteger(0);

    // Private constructor - only this class can create an instance.
    private IdGenerator() {
    }

    public static IdGenerator getInstance() {
        if (instance == null) {                    // 1st check (no locking, fast path)
            synchronized (IdGenerator.class) {
                if (instance == null) {             // 2nd check (inside lock)
                    instance = new IdGenerator();
                }
            }
        }
        return instance;
    }

    public String nextDoctorId() {
        return String.format("DOC-%04d", doctorSeq.incrementAndGet());
    }

    public String nextPatientId() {
        return String.format("PAT-%04d", patientSeq.incrementAndGet());
    }

    public String nextAppointmentId() {
        return String.format("APT-%04d", appointmentSeq.incrementAndGet());
    }
}