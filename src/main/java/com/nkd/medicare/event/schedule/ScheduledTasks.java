package com.nkd.medicare.event.schedule;

import com.nkd.medicare.enums.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.nkd.medicare.Tables.APPOINTMENT;

@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final DSLContext context;

    @Scheduled(fixedRate = 3600000)
    public void removeExpiredAppointments(){
        context.update(APPOINTMENT)
                .set(APPOINTMENT.STATUS, AppointmentStatus.NOT_SHOWED_UP)
                .where(APPOINTMENT.DATE.lt(LocalDate.now()))
                    .or(APPOINTMENT.DATE.eq(LocalDate.now()).and(APPOINTMENT.TIME.lt(LocalTime.now().plusHours(1))))
                    .and(APPOINTMENT.STATUS.ne(AppointmentStatus.DONE).and(APPOINTMENT.STATUS.ne(AppointmentStatus.CANCELLED)))
                .execute();
    }
}
