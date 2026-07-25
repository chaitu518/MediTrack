package com.airtribe.meditrack.constants;

public class Constants {
    public static final String filePath = "https://api.airtribe.com";
    public static final double TAX_RATE = 0.01;

    /**
     * Minimum gap, in minutes, required between two non-cancelled
     * appointments for the same doctor (or same patient) - the basis
     * for double-booking / time-slot conflict checks.
     */
    public static final int APPOINTMENT_SLOT_MINUTES = 30;
}
