package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.interfaces.Searchable;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.Validator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CRUD + search over Doctor. Implements {@code Searchable<Doctor>} and adds
 * overloaded searchDoctor() variants - overloading (compile-time
 * polymorphism) alongside the interface's overridden runtime behavior.
 */
public class DoctorService implements Searchable<Doctor> {

    private final DataStore<Doctor> store = new DataStore<>(Doctor::getId);

    public Doctor addDoctor(String name, int age, String phone,
                            Specialization specialization, double fee) throws InvalidDataException {
        Validator.validateName(name);
        Validator.validateAge(age);
        Validator.validatePhone(phone);
        Validator.validateFee(fee);

        String id = IdGenerator.getInstance().nextDoctorId();
        Doctor doctor = new Doctor(id, name, age, phone, specialization, fee);
        store.save(doctor);
        return doctor;
    }

    public boolean updateDoctor(Doctor doctor) {
        if (!store.exists(doctor.getId())) {
            return false;
        }
        store.save(doctor);
        return true;
    }

    public boolean deleteDoctor(String id) {
        return store.deleteById(id);
    }

    public List<Doctor> getAllDoctors() {
        return store.findAll();
    }


    @Override
    public Doctor searchById(String id) {
        return store.findById(id);
    }

    @Override
    public List<Doctor> searchByName(String name) {
        return store.findAll().stream()
                .filter(d -> matches(d.getName(), name))
                .collect(Collectors.toList());
    }


    public Doctor searchDoctor(String id) {
        return searchById(id);
    }

    public List<Doctor> searchDoctor(Specialization specialization) {
        return store.findAll().stream()
                .filter(d -> d.getSpecialization() == specialization)
                .collect(Collectors.toList());
    }

    public List<Doctor> searchDoctor(int minAge, int maxAge) {
        return store.findAll().stream()
                .filter(d -> d.getAge() >= minAge && d.getAge() <= maxAge)
                .collect(Collectors.toList());
    }


    public double averageFee() {
        return store.findAll().stream()
                .mapToDouble(Doctor::getConsultationFee)
                .average()
                .orElse(0.0);
    }

    public List<Doctor> sortedByFee() {
        return store.findAll().stream()
                .sorted(Comparator.comparingDouble(Doctor::getConsultationFee))
                .collect(Collectors.toList());
    }

    public Map<Specialization, Long> countBySpecialization() {
        return store.findAll().stream()
                .collect(Collectors.groupingBy(Doctor::getSpecialization, Collectors.counting()));
    }
}