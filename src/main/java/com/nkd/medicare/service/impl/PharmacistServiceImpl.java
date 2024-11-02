package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.PharmacistSignal;
import com.nkd.medicare.enums.PrescribedStatus;
import com.nkd.medicare.service.PharmacistService;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class PharmacistServiceImpl implements PharmacistService {

    private final DSLContext context;

    @Override
    public String fetchStaffData(String staffID) {
        return context.select(STAFF.STAFF_IMAGE)
                .from(STAFF)
                .where(STAFF.STAFF_ID.eq(Integer.valueOf(staffID)))
                .fetchOneInto(String.class);
    }

    @Override
    public PharmacistSignal completePrescribed(String prescribedID) {
        int c = context.update(PRESCRIBED)
                .set(PRESCRIBED.STATUS, PrescribedStatus.DONE)
                .where(PRESCRIBED.PRESCRIBED_ID.eq(Integer.parseInt(prescribedID)))
                .execute();
        String signalID = c==0?"PH_03":"PH_01";
        return new PharmacistSignal(signalID, Map.of("presribedID",prescribedID));
    }

}
