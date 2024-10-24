package com.nkd.medicare.service.impl;

import com.nkd.medicare.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import static com.nkd.medicare.Tables.STAFF;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final DSLContext context;

    @Override
    public String fetchStaffData(String staffID) {
        System.out.println("call fetch staff data");
        return context.select(STAFF.STAFF_IMAGE)
                .from(STAFF)
                .where(STAFF.STAFF_ID.eq(Integer.valueOf(staffID)))
                .fetchOneInto(String.class);
    }
}
