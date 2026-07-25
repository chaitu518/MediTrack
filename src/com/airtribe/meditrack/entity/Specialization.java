package com.airtribe.meditrack.entity;

public enum Specialization {
    CARDIOLOGY("Cardiology"),
    DERMATOLOGY("Dermatology"),
    NEUROLOGY("Neurology"),
    PEDIATRICS("Pediatrics"),
    ORTHOPEDICS("Orthopedics"),
    GYNECOLOGY("Gynecology"),
    PSYCHIATRY("Psychiatry"),
    ONCOLOGY("Oncology"),
    RADIOLOGY("Radiology"),
    UROLOGY("Urology");
    private final String displayName;
    Specialization(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
    @Override
    public String toString() {
        return displayName;
    }
}
