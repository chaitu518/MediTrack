package com.airtribe.meditrack.util;

import com.airtribe.meditrack.exception.InvalidDataException;

/**
 * Centralized validation used by entity setters/constructors and by
 * CSVUtil when parsing rows. Keeping this in one place means validation
 * rules never drift between, say, Doctor and Patient.
 */
public final class Validator {

    private Validator() {
    }

    public static void validateName(String name) throws InvalidDataException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidDataException("Name must not be blank.");
        }
    }

    public static void validateAge(int age) throws InvalidDataException {
        if (age <= 0 || age > 130) {
            throw new InvalidDataException("Age must be between 1 and 130, got: " + age);
        }
    }

    public static void validatePhone(String phone) throws InvalidDataException {
        if (phone == null || !phone.matches("\\d{10}")) {
            throw new InvalidDataException("Phone must be exactly 10 digits, got: " + phone);
        }
    }

    public static void validateFee(double fee) throws InvalidDataException {
        if (fee < 0) {
            throw new InvalidDataException("Fee cannot be negative, got: " + fee);
        }
    }

    public static void validateId(String id) throws InvalidDataException {
        if (id == null || id.trim().isEmpty()) {
            throw new InvalidDataException("ID must not be blank.");
        }
    }
}
