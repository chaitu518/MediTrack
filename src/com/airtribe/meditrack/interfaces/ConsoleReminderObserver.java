package com.airtribe.meditrack.interfaces;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.service.AppointmentObserver;
import com.airtribe.meditrack.util.DateUtil;

/**
 * Simplest possible Observer: prints a reminder line to the console.
 */
public class ConsoleReminderObserver implements AppointmentObserver {

    @Override
    public void onAppointmentCreated(Appointment appointment) {
        System.out.println("[Reminder] New appointment " + appointment.getId()
                + " scheduled for " + DateUtil.format(appointment.getDateTime()));
    }

    @Override
    public void onAppointmentCancelled(Appointment appointment) {
        System.out.println("[Reminder] Appointment " + appointment.getId() + " was cancelled.");
    }
}
