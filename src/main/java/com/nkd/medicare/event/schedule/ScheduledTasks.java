package com.nkd.medicare.event.schedule;

import com.nkd.medicare.enums.AppointmentStatus;
import com.nkd.medicare.service.EmailService;
import com.nkd.medicare.tables.records.AppointmentRemindersRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.nkd.medicare.Tables.APPOINTMENT;
import static com.nkd.medicare.Tables.APPOINTMENT_REMINDERS;

@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final DSLContext context;
    private final EmailService emailService;

    @Scheduled(fixedRate = 3600000)
    public void removeExpiredAppointments(){
        context.update(APPOINTMENT)
                .set(APPOINTMENT.STATUS, AppointmentStatus.NOT_SHOWED_UP)
                .where(APPOINTMENT.DATE.lt(LocalDate.now()))
                    .or(APPOINTMENT.DATE.eq(LocalDate.now()).and(APPOINTMENT.TIME.eq(LocalTime.now().minusHours(1))))
                    .and(APPOINTMENT.STATUS.ne(AppointmentStatus.DONE).and(APPOINTMENT.STATUS.ne(AppointmentStatus.CANCELLED)))
                .execute();
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void reminderAppointments(){
        List<AppointmentRemindersRecord> reminders = context.selectFrom(APPOINTMENT_REMINDERS)
                .where(APPOINTMENT_REMINDERS.DATE.eq(LocalDate.now().minusDays(1)))
                .fetch();

        for (AppointmentRemindersRecord record : reminders) {
            emailService.sendReminderEmail(record.getTargetAddress(), record.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                    record.getTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }
}
