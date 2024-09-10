package com.nkd.medicare.service.impl;

import com.nkd.medicare.enums.StaffStaffType;
import com.nkd.medicare.service.UserService;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final DSLContext context;

    @Override
    public String getStaffData(String name, String department, String primaryLanguage, String specialization, String gender, String pageSize, String pageNumber) {
        Condition condition = DSL.trueCondition();

        if(department != null && !department.isEmpty() && !department.equals("default")){
            condition = condition.and(DEPARTMENT.NAME.eq(department));
        }
        if(primaryLanguage != null && !primaryLanguage.isEmpty() && !primaryLanguage.equals("default")){
            condition = condition.and(PERSON.PRIMARY_LANGUAGE.eq(primaryLanguage));
        }
        if(specialization != null && !specialization.isEmpty() && !specialization.equals("default")){
            condition = condition.and(SPECIALIZATION.NAME.eq(specialization));
        }
        if(gender != null && !gender.isEmpty()){
            condition = condition.and(PERSON.GENDER.eq(gender));
        }

        return context.select(STAFF.STAFF_ID, PERSON.PERSON_ID, STAFF.STAFF_IMAGE, STAFF.DEPARTMENT_ID, PERSON.FIRST_NAME,
                        PERSON.LAST_NAME, PERSON.PHONE_NUMBER, PERSON.GENDER, PERSON.PRIMARY_LANGUAGE, DEPARTMENT.NAME, DEPARTMENT.LOCATION, SPECIALIZATION.NAME,
                        SPECIALIZATION.DESCRIPTION)
                .from(STAFF.join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID))
                .join(DEPARTMENT).on(STAFF.DEPARTMENT_ID.eq(DEPARTMENT.DEPARTMENT_ID))
                .leftJoin(STAFF_SPECIALIZATION).on(STAFF.STAFF_ID.eq(STAFF_SPECIALIZATION.STAFF_ID))
                .join(SPECIALIZATION).on(STAFF_SPECIALIZATION.SPECIALIZATION_ID.eq(SPECIALIZATION.SPECIALIZATION_ID)))
                .where (
                    (PERSON.FIRST_NAME.like("%" + name + "%").or(PERSON.LAST_NAME.like("%" + name + "%")))
                    .and(STAFF.STAFF_TYPE.eq(StaffStaffType.DOCTOR))
                    .and(condition))
                .limit(Integer.parseInt(pageSize))
                .offset(((Integer.parseInt(pageNumber) - 1) * Integer.parseInt(pageSize)))
                .fetch()
                .formatJSON();
    }
}
