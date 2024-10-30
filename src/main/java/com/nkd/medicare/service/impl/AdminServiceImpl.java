package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.StaffDTO;
import com.nkd.medicare.enums.AccountAccountRole;
import com.nkd.medicare.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.format.DateTimeFormatter;

import static com.nkd.medicare.Tables.*;
import static com.nkd.medicare.Tables.PERSON;
import static org.jooq.impl.DSL.asterisk;

import com.nkd.medicare.domain.StaffExcelData;
import com.nkd.medicare.enums.StaffStaffType;
import com.nkd.medicare.tables.records.*;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final DSLContext context;
    private final PasswordEncoder encoder;

    public String getStaff(String name, String department, String primaryLanguage, String specialization
            , String gender, String pageSize, String pageNumber, String staffType, String status) {
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
        if(status != null && !status.isEmpty() && !status.equals("default")){
            condition = condition.and(STAFF.EMP_STATUS.eq(status));
        }
        if(staffType != null && !staffType.isEmpty()){
            if(staffType.compareTo("nurse") == 0) {
                condition = condition.and(STAFF.STAFF_TYPE.eq(StaffStaffType.NURSE));
            }
            else if(staffType.compareTo("doctor") == 0) {
                condition = condition.and(STAFF.STAFF_TYPE.eq(StaffStaffType.DOCTOR));
            }
            else if(staffType.compareTo("pharmacist") == 0) {
                condition = condition.and(STAFF.STAFF_TYPE.eq(StaffStaffType.PHARMACIST));
            }
            else if(staffType.compareTo("admin") == 0){
                condition = condition.and(STAFF.STAFF_TYPE.eq(StaffStaffType.ADMIN));
            }
        }

        return context.select(STAFF.STAFF_ID, PERSON.FIRST_NAME, PERSON.LAST_NAME, PERSON.PHONE_NUMBER, PERSON.GENDER,
                        DEPARTMENT.NAME, SPECIALIZATION.NAME, STAFF.STAFF_TYPE, ACCOUNT.ACCOUNT_EMAIL, STAFF.EMP_STATUS)
                .from(STAFF.join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID))
                        .join(DEPARTMENT).on(STAFF.DEPARTMENT_ID.eq(DEPARTMENT.DEPARTMENT_ID))
                        .leftJoin(STAFF_SPECIALIZATION).on(STAFF.STAFF_ID.eq(STAFF_SPECIALIZATION.STAFF_ID))
                        .join(SPECIALIZATION).on(STAFF_SPECIALIZATION.SPECIALIZATION_ID.eq(SPECIALIZATION.SPECIALIZATION_ID))
                        .join(ACCOUNT).on(ACCOUNT.OWNER_ID.eq(STAFF.STAFF_ID)))
                .where
                        ((PERSON.FIRST_NAME.like("%" + name + "%").or(PERSON.LAST_NAME.like("%" + name + "%"))
                                .or(PERSON.FIRST_NAME.concat(" ").concat(PERSON.LAST_NAME).like("%" + name + "%")))
                            .and(condition)
                            .and(ACCOUNT.ACCOUNT_ROLE.ne(AccountAccountRole.USER)))
                .limit(Integer.parseInt(pageSize))
                .offset(((Integer.parseInt(pageNumber) - 1) * Integer.parseInt(pageSize)))
                .fetch()
                .formatJSON();
    }

    @Override
    public List<StaffExcelData> readFromExcel(String url) {
        StaffExcelData staffData;
        List<StaffExcelData> returnList = new ArrayList<>();
        try {
            InputStream inputStream = URI.create(url).toURL().openStream();
            byte[] fileData = IOUtils.toByteArray(inputStream);
            InputStream byteInputStream = new ByteArrayInputStream(fileData);

            Workbook workbook = new XSSFWorkbook(byteInputStream);
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                staffData = addStaff(row);
                if (staffData != null) returnList.add(staffData);
            }
            workbook.close();
            byteInputStream.close();
            inputStream.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return returnList;
    }

    public StaffExcelData addStaff(Row row) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        ArrayList<?> rowData = extractData(row);
        StaffExcelData data = new StaffExcelData();
        if(rowData.getLast().toString().compareTo("2") == 0){
            data.setResultType(2);
            data.setEmail(rowData.get(0) != null ? rowData.get(0).toString() : "");
            data.setDateOfBirth(LocalDate.now());
            data.setPassword(rowData.get(1) != null ? rowData.get(1).toString() : "");
            data.setFirstname(rowData.get(2) != null ? rowData.get(2).toString() : "");
            data.setLastname(rowData.get(3) != null ? rowData.get(3).toString() : "");
            data.setCCCD(rowData.get(5) != null ? rowData.get(5).toString() : "");
            data.setPhoneNumber(rowData.get(6) != null ? rowData.get(6).toString() : "");
            data.setStaffID(null);
            data.setRole(null);
            return data;
        }
        else {
            var existID = context.select(ACCOUNT)
                    .from(ACCOUNT)
                    .where(ACCOUNT.ID_CARD_NUMBER.eq(rowData.get(5).toString()))
                    .fetch();
            if(!existID.isEmpty()){
                data.setResultType(3);
                data.setEmail(rowData.get(0) != null ? rowData.get(0).toString() : "");
                data.setDateOfBirth(LocalDate.parse(rowData.get(4).toString(), formatter));
                data.setPassword(rowData.get(1) != null ? rowData.get(1).toString() : "");
                data.setFirstname(rowData.get(2) != null ? rowData.get(2).toString() : "");
                data.setLastname(rowData.get(3) != null ? rowData.get(3).toString() : "");
                data.setCCCD(rowData.get(5) != null ? rowData.get(5).toString() : "");
                data.setPhoneNumber(rowData.get(6) != null ? rowData.get(6).toString() : "");
                data.setStaffID(null);
                data.setRole(null);
                return data;
            }
            var existEmail = context.select(ACCOUNT)
                    .from(ACCOUNT)
                    .where(ACCOUNT.ACCOUNT_EMAIL.eq(rowData.get(0).toString()))
                    .fetch();
            if(!existEmail.isEmpty()){
                data.setResultType(3);
                data.setDateOfBirth(LocalDate.parse(rowData.get(4).toString(), formatter));
                data.setEmail(rowData.get(0) != null ? rowData.get(0).toString() : "");
                data.setPassword(rowData.get(1) != null ? rowData.get(1).toString() : "");
                data.setFirstname(rowData.get(2) != null ? rowData.get(2).toString() : "");
                data.setLastname(rowData.get(3) != null ? rowData.get(3).toString() : "");
                data.setCCCD(rowData.get(5) != null ? rowData.get(5).toString() : "");
                data.setPhoneNumber(rowData.get(6) != null ? rowData.get(6).toString() : "");
                data.setStaffID(null);
                data.setRole(null);
                return data;
            }
            else {
                AddressRecord address = new AddressRecord();
                address.setProvince(rowData.get(21).toString());
                address.setCity(rowData.get(20).toString());
                address.setDistrict(rowData.get(19).toString());
                address.setStreet(rowData.get(18).toString());
                address.setHouseNumber(rowData.get(17).toString());
                int addressID = Objects.requireNonNull(context.insertInto(ADDRESS).set(address)
                        .returning(ADDRESS.ADDRESS_ID).fetchOne()).getAddressId();

                PersonRecord person = new PersonRecord();
                person.setFirstName(rowData.get(2).toString());
                person.setLastName(rowData.get(3).toString());
                person.setDateOfBirth(LocalDate.parse(rowData.get(4).toString(), formatter));
                person.setPhoneNumber(rowData.get(6).toString());
                person.setSecPhoneNumber(rowData.get(7).toString());
                person.setGender(rowData.get(8).equals(1) ? "Male" : "Female");
                person.setPrimaryLanguage(rowData.get(9).toString());
                person.setAddressId(addressID);
                int personID = Objects.requireNonNull(context.insertInto(PERSON).set(person)
                        .returning(PERSON.PERSON_ID).fetchOne()).getPersonId();

                StaffRecord staff = new StaffRecord();
                staff.setPersonId(personID);
                staff.setStaffImage(rowData.get(10).toString());
                staff.setDepartmentId(Integer.parseInt(rowData.get(12).toString()));
                staff.setLicenseType(rowData.get(13).toString());
                staff.setLicenseNumber(rowData.get(14).toString());
                staff.setLicenseExpiryDate(LocalDate.parse(rowData.get(15).toString(), formatter));
                staff.setEmpStartDate(LocalDate.now());
                staff.setEmpStatus("Active");

                staff.setLastInfoUpdate(LocalDateTime.now());
                AccountAccountRole role;
                if (rowData.get(11).toString().compareTo("NURSE") == 0) {
                    staff.setStaffType(StaffStaffType.NURSE);
                    role = AccountAccountRole.NURSE;
                } else if (rowData.get(11).toString().compareTo("DOCTOR") == 0) {
                    role = AccountAccountRole.DOCTOR;
                    staff.setStaffType(StaffStaffType.DOCTOR);
                } else if (rowData.get(11).toString().compareTo("PHARMACIST") == 0) {
                    role = AccountAccountRole.PHARMACIST;
                    staff.setStaffType(StaffStaffType.PHARMACIST);
                } else if (rowData.get(11).toString().compareTo("ADMIN") == 0) {
                    role = AccountAccountRole.ADMIN;
                    staff.setStaffType(StaffStaffType.ADMIN);
                }
                else return null;
                int staffID = Objects.requireNonNull(context.insertInto(STAFF).set(staff)
                        .returning(STAFF.STAFF_ID).fetchOne()).getStaffId();

                StaffSpecializationRecord staffSpecialization = new StaffSpecializationRecord();
                staffSpecialization.setStaffId(staffID);
                staffSpecialization.setSpecializationId(Integer.parseInt(rowData.get(16).toString()));
                context.insertInto(STAFF_SPECIALIZATION).set(staffSpecialization).execute();

                AccountRecord newAccount = new AccountRecord();
                newAccount.setAccountEmail(rowData.get(0).toString());
                newAccount.setAccountPassword(encoder.encode(rowData.get(1).toString()));
                newAccount.setIdCardNumber(rowData.get(5).toString());
                newAccount.setAccountRole(role);
                newAccount.setIsLocked((byte) 0);
                newAccount.setIsEnable((byte) 1);
                newAccount.setIsCredentialNonExpired((byte) 1);
                newAccount.setOwnerId(staffID);
                int accountID = Objects.requireNonNull(context.insertInto(ACCOUNT).set(newAccount)
                        .returning(ACCOUNT.ACCOUNT_ID).fetchOne()).getAccountId();

                data.setResultType(1);
                data.setAccountID(accountID + "");
                data.setDateOfBirth(LocalDate.parse(rowData.get(4).toString(), formatter));
                data.setEmail(newAccount.getAccountEmail());
                data.setPassword(rowData.get(1).toString());
                data.setFirstname(person.getFirstName());
                data.setLastname(person.getLastName());
                data.setCCCD(rowData.get(5).toString());
                data.setPhoneNumber(person.getPhoneNumber());
                data.setStaffID(staffID + "");
                data.setRole(role);
                System.out.println("alo5");
                return data;
            }
        }
    }

    @Override
    public void deleteStaff(String staffID, String note) {
        context.update(STAFF)
                .set(STAFF.EMP_STATUS, "Inactive")
                .set(STAFF.NOTE, note);
                .where(STAFF.STAFF_ID.eq(Integer.parseInt(staffID)))
                .execute();
    }

    @Override
    public String getStaffByID(String staffID) {
        return context.select(asterisk())
                .from(STAFF.join(PERSON).on(STAFF.PERSON_ID.eq(PERSON.PERSON_ID))
                        .join(ADDRESS).on(PERSON.ADDRESS_ID.eq(ADDRESS.ADDRESS_ID))
                        .join(DEPARTMENT).on(STAFF.DEPARTMENT_ID.eq(DEPARTMENT.DEPARTMENT_ID))
                        .leftJoin(STAFF_SPECIALIZATION).on(STAFF.STAFF_ID.eq(STAFF_SPECIALIZATION.STAFF_ID))
                        .join(SPECIALIZATION).on(STAFF_SPECIALIZATION.SPECIALIZATION_ID.eq(SPECIALIZATION.SPECIALIZATION_ID))
                        .join(ACCOUNT).on(ACCOUNT.OWNER_ID.eq(STAFF.STAFF_ID))
                .where(STAFF.STAFF_ID.eq(Integer.parseInt(staffID)).and(ACCOUNT.ACCOUNT_ROLE.ne(AccountAccountRole.USER))))
                .fetch()
                .formatJSON();
    }

    @Override
    public void updateStaff(StaffDTO staffDTO) {
        System.out.println(staffDTO.toString());
        context.update(STAFF)
                .set(STAFF.STAFF_TYPE, StaffStaffType.valueOf(staffDTO.getStaffType().toUpperCase()))
                .set(STAFF.EMP_END_DATE, staffDTO.getEndDate())
                .where(STAFF.STAFF_ID.eq(Integer.parseInt(staffDTO.getStaffID())))
                .execute();
        context.update(ACCOUNT)
                .set(ACCOUNT.ACCOUNT_EMAIL, staffDTO.getEmail())
                .set(ACCOUNT.ID_CARD_NUMBER, staffDTO.getIdNumber())
                .set(ACCOUNT.ACCOUNT_ROLE, AccountAccountRole.valueOf(staffDTO.getStaffType().toUpperCase()))
                .where(ACCOUNT.ACCOUNT_ID.eq(Integer.parseInt(staffDTO.getAccountID())))
                .execute();
        context.update(PERSON)
                .set(PERSON.FIRST_NAME, staffDTO.getFirstName())
                .set(PERSON.LAST_NAME, staffDTO.getLastName())
                .set(PERSON.DATE_OF_BIRTH, staffDTO.getDateOfBirth())
                .set(PERSON.PHONE_NUMBER, staffDTO.getPhoneNumber())
                .set(PERSON.SEC_PHONE_NUMBER, staffDTO.getSecPhoneNumber())
                .set(PERSON.PRIMARY_LANGUAGE, staffDTO.getPrimaryLanguage())
                .where(PERSON.PERSON_ID.eq(Integer.parseInt(staffDTO.getPersonID())))
                .execute();
        context.update(ADDRESS)
                .set(ADDRESS.HOUSE_NUMBER, staffDTO.getHouseNumber())
                .set(ADDRESS.STREET, staffDTO.getStreet())
                .set(ADDRESS.DISTRICT, staffDTO.getDistrict())
                .set(ADDRESS.CITY, staffDTO.getCity())
                .set(ADDRESS.PROVINCE, staffDTO.getProvince())
                .where(ADDRESS.ADDRESS_ID.eq(Integer.parseInt(staffDTO.getAddressID())))
                .execute();
    }

    @Override
    public void updateStaffProfileImage(String staffID, String imageURL) {
        context.update(STAFF)
                .set(STAFF.STAFF_IMAGE, imageURL)
                .where(STAFF.STAFF_ID.eq(Integer.parseInt(staffID)))
                .execute();
    }

    private ArrayList<?> extractData(Row row) {
        int checkdata = 0;
        ArrayList<Object> data = new ArrayList<>();
        for (Cell cell : row) {
            switch (cell.getCellType()) {
                case STRING:
                    data.add(cell.getStringCellValue());
                    checkdata++;
                    break;
                case BLANK:
                    data.add("");
                    break;
                case NUMERIC:
                    double temp = cell.getNumericCellValue();
                    data.add((int) temp);
                    checkdata++;
                    break;
                default:
            }
        }
        if (checkdata == 22) return data;
        else {
            data.add(2);
            return data;
        }
    }
}
