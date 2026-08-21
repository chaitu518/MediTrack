package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.util.Validator;

public abstract class Person extends MedicalEntity {
    private String name;
    private int age;
    private String phoneNumber;

    public Person(String id,String name, int age, String phoneNumber) {
        super(id);
        this.name = name;
        this.age = age;
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidDataException {
        Validator.validateName(name);
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) throws InvalidDataException {
        Validator.validateAge(age);
        this.age = age;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) throws InvalidDataException {
        Validator.validatePhone(phoneNumber);
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String summary() {
        return String.format("[%s] %s (age %d, phone %s)", id, name, age, phoneNumber);
    }
}