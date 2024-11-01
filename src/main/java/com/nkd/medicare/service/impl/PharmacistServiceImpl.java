package com.nkd.medicare.service.impl;

import com.nkd.medicare.service.PharmacistService;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

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

}
