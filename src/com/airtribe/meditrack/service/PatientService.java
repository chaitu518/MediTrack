package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.interfaces.Searchable;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.Validator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CRUD + search over Patient. Mirrors DoctorService's shape
 * (overloaded searchPatient by id / name / age) intentionally -
 * consistent service API across the app.
 */
public class PatientService implements Searchable<Patient> {

    private final DataStore<Patient> store = new DataStore<>(Patient::getId);

    public Patient addPatient(String name, int age, String phone, String bloodGroup, boolean hasInsurance) throws InvalidDataException {
        Validator.validateName(name);
        Validator.validateAge(age);
        Validator.validatePhone(phone);

        String id = IdGenerator.getInstance().nextPatientId();
        Patient patient = new Patient(id, name, age, phone, bloodGroup, hasInsurance);
        store.save(patient);
        return patient;
    }

    public boolean updatePatient(Patient patient) {
        if (!store.exists(patient.getId())) {
            return false;
        }
        store.save(patient);
        return true;
    }

    public boolean deletePatient(String id) {
        return store.deleteById(id);
    }

    public List<Patient> getAllPatients() {
        return store.findAll();
    }

    /**
     * Returns a deep-cloned copy of a patient's record - safe to hand to
     * a caller who might mutate it without risking the store's own data.
     */
    public Patient getSafeCopy(String id) {
        Patient original = store.findById(id);
        return original == null ? null : original.clone();
    }


    @Override
    public Patient searchById(String id) {
        return store.findById(id);
    }

    @Override
    public List<Patient> searchByName(String name) {
        return store.findAll().stream()
                .filter(p -> matches(p.getName(), name))
                .collect(Collectors.toList());
    }


    public Patient searchPatient(String id) {
        return searchById(id);
    }

    public List<Patient> searchPatient(int age) {
        return store.findAll().stream()
                .filter(p -> p.getAge() == age)
                .collect(Collectors.toList());
    }

    public List<Patient> searchPatientByName(String name) {
        return searchByName(name);
    }
}