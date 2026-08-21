package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Appointment;


public interface AppointmentObserver {
    void onAppointmentCreated(Appointment appointment);

    void onAppointmentCancelled(Appointment appointment);
}