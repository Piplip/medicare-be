package com.nkd.medicare.service.impl;

import com.nkd.medicare.domain.DataExcelComplete;
import com.nkd.medicare.enums.AccountAccountRole;
import com.nkd.medicare.enums.StaffStaffType;
import com.nkd.medicare.service.AdminService;
import com.nkd.medicare.tables.records.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jooq.DSLContext;
import org.jooq.Null;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.nkd.medicare.Tables.*;

@Service
@RequiredArgsConstructor
public class AdminServicelmpl implements AdminService {
    private final DSLContext context;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private int checkpatient2 = 0;
    private final PasswordEncoder encoder;
    private AccountAccountRole role;
    @Override
    public Map<String, DataExcelComplete> readexcel(String url) {
        DataExcelComplete data1 = new DataExcelComplete();
        Map<String, DataExcelComplete> dulieuthanhcong = new HashMap<>();
        String fileUrl = url;
        try {
            InputStream inputStream = new URL(fileUrl).openStream();
            byte[] fileData = IOUtils.toByteArray(inputStream);
            InputStream byteInputStream = new ByteArrayInputStream(fileData);

            Workbook workbook = new XSSFWorkbook(byteInputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                Cell cell = row.getCell(0);
                    switch (cell.getStringCellValue()) {
                        case "add":
                            data1 = addstaff(row);
                            if(data1 != null) dulieuthanhcong.put("add",data1);
                            break;
                        case "delete":
                            data1 = delstaff(row);
                            if(data1 != null) dulieuthanhcong.put("del",data1);
                            break;
                        default:
                    }
                }
            workbook.close();
            byteInputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dulieuthanhcong;
    }
    public DataExcelComplete addstaff(Row row) {
        ArrayList adddetail = new ArrayList<>();
            for (Cell cell : row) {
                switch (cell.getCellType()) {
                    case STRING:
                        adddetail.add(cell.getStringCellValue());
                        break;
                    case BLANK:
                        break;
                    case NUMERIC:
                        adddetail.add(cell.getNumericCellValue());
                        break;
                    default:
                }
            }
        DataExcelComplete data = new DataExcelComplete();

        AddressRecord address = new AddressRecord();
        address.setProvince(adddetail.get(5).toString());
        address.setCity(adddetail.get(4).toString());
        address.setDistrict(adddetail.get(3).toString());
        address.setStreet(adddetail.get(2).toString());
        address.setHouseNumber(adddetail.get(1).toString());
        int addressID = Objects.requireNonNull(context.insertInto(ADDRESS).set(address)
                .returning(ADDRESS.ADDRESS_ID).fetchOne()).getAddressId();

        PersonRecord person = new PersonRecord();
        person.setFirstName(adddetail.get(6).toString());
        person.setLastName(adddetail.get(7).toString());
        person.setDateOfBirth(LocalDate.parse(adddetail.get(8).toString(), formatter));
        person.setPhoneNumber(adddetail.get(9).toString());
        person.setSecPhoneNumber(adddetail.get(10).toString());
        person.setGender(adddetail.get(11).toString());
        person.setPrimaryLanguage(adddetail.get(12).toString());
        person.setAddressId(addressID);
        int personID = Objects.requireNonNull(context.insertInto(PERSON).set(person)
                .returning(PERSON.PERSON_ID).fetchOne()).getPersonId();

        StaffRecord staff = new StaffRecord();
        staff.setPersonId(personID);
        staff.setStaffImage(adddetail.get(13).toString());
        if(adddetail.get(14).toString().compareTo("NURSE") == 0) {
            staff.setStaffType(StaffStaffType.NURSE);
            role = AccountAccountRole.NURSE;
        }
        else if(adddetail.get(14).toString().compareTo("DOCTOR") == 0) {
            role = AccountAccountRole.DOCTOR;
            staff.setStaffType(StaffStaffType.DOCTOR);
        }
        else if(adddetail.get(14).toString().compareTo("PHARMACIST") == 0) {
            role = AccountAccountRole.PHARMACIST;
            staff.setStaffType(StaffStaffType.PHARMACIST);
        }
        else return null;
        staff.setDepartmentId(Integer.parseInt(adddetail.get(15).toString()));
        staff.setLicenseType(adddetail.get(16).toString());
        staff.setLicenseNumber(adddetail.get(17).toString());
        staff.setLicenseExpiryDate(LocalDate.parse(adddetail.get(18).toString(), formatter));
        staff.setEmpStartDate(LocalDate.parse(adddetail.get(19).toString(), formatter));
        staff.setEmpEndDate(LocalDate.parse(adddetail.get(20).toString(), formatter));
        staff.setEmpStatus(adddetail.get(21).toString());
        staff.setLastInfoUpdate(LocalDateTime.now());
        int staffID = Objects.requireNonNull(context.insertInto(STAFF).set(staff)
                .returning(STAFF.STAFF_ID).fetchOne()).getStaffId();

        StaffSpecializationRecord staffspecie = new StaffSpecializationRecord();
        staffspecie.setStaffId(staffID);
        staffspecie.setSpecializationId(Integer.parseInt(adddetail.get(22).toString()));
        context.insertInto(STAFF_SPECIALIZATION).set(staffspecie).execute();

        AccountRecord newAccount = new AccountRecord();
        newAccount.setAccountEmail(adddetail.get(23).toString());
        newAccount.setAccountPassword(encoder.encode(adddetail.get(24).toString()));
        newAccount.setIdCardNumber(adddetail.get(25).toString());
        newAccount.setAccountRole(role);
        newAccount.setIsLocked((byte)0);
        newAccount.setIsEnable((byte)1);
        newAccount.setIsCredentialNonExpired((byte)1);
        newAccount.setOwnerId(personID);
        int accountID = Objects.requireNonNull(context.insertInto(ACCOUNT).set(newAccount)
                .returning(ACCOUNT.ACCOUNT_ID).fetchOne()).getAccountId();

        data.setAccountID(accountID+"");
        data.setEmail(newAccount.getAccountEmail());
        data.setPassword(adddetail.get(24).toString());
        data.setFirstname(person.getFirstName());
        data.setLastname(person.getLastName());
        data.setCCCD(adddetail.get(25).toString());
        data.setPhonenumber(person.getPhoneNumber());
        data.setDateofbirth(person.getDateOfBirth());
        data.setStaffid(staffID+"");
        data.setRole(role);

        return data;
    }
    public DataExcelComplete delstaff(Row row) {
        ArrayList adddetail = new ArrayList<>();
        for (Cell cell : row) {
            switch (cell.getCellType()) {
                case STRING:
                    adddetail.add(cell.getStringCellValue());
                    break;
                case BLANK:
                    break;
                case NUMERIC:
                    adddetail.add(cell.getNumericCellValue());
                    break;
                default:
            }
        }
        DataExcelComplete data = new DataExcelComplete();
        PersonRecord personRecord = context.selectFrom(PERSON)
                .where(PERSON.PERSON_ID.eq(Integer.parseInt(adddetail.get(1).toString())))
                .or(
                         PERSON.FIRST_NAME.eq(adddetail.get(2).toString())
                        .and(PERSON.LAST_NAME.eq(adddetail.get(3).toString())))
                .fetchOne();
        if(personRecord.getPersonId() != null) {
            context.deleteFrom(ADDRESS).where(ADDRESS.ADDRESS_ID.eq(personRecord.getAddressId())).execute();
            context.deleteFrom(PERSON).where(PERSON.PERSON_ID.eq(personRecord.getPersonId())).execute();
            context.deleteFrom(ACCOUNT).where(ACCOUNT.OWNER_ID.eq(personRecord.getPersonId())).execute();
            Integer checkpatient = context.selectCount().from(PATIENT)
                    .where(PATIENT.PERSON_ID.eq(personRecord.getPersonId()))
                    .fetchOneInto(Integer.class);
            checkpatient2 = checkpatient != null ? checkpatient2 : 0;
            if (checkpatient2 != 0) {
                context.deleteFrom(PATIENT).where(PATIENT.PERSON_ID.eq(personRecord.getPersonId())).execute();
            } else {
                int staffID = (context.selectFrom(STAFF).where(STAFF.PERSON_ID.eq(personRecord.getPersonId())).fetchOne()).getStaffId();
                context.deleteFrom(STAFF_SPECIALIZATION).where(STAFF_SPECIALIZATION.STAFF_ID.eq(staffID)).execute();
                context.deleteFrom(STAFF).where(STAFF.PERSON_ID.eq(personRecord.getPersonId())).execute();
            }
            data.setAccountID(adddetail.get(1).toString());
            data.setFirstname(adddetail.get(2).toString());
            data.setLastname(adddetail.get(3).toString());
            return data;
        }
        else return null;
    }
    public boolean updatestaff(Row row){
//        ArrayList adddetail = new ArrayList<>();
//        for(Cell cell : row){
//            if(cell.getStringCellValue().compareTo("") == 0) adddetail.add("default");
//            else adddetail.add(cell.getStringCellValue());
//        }
//        context.update(PERSON)
//                .set(PERSON.PERSON_ID, adddetail.get(1))
//                .set(PAYMENT.TRANSACTION_STATUS, PaymentTransactionStatus.COMPLETED)
//                .where(PAYMENT.APPOINTMENT_ID.eq(Integer.parseInt(appointmentID)))
//                .execute();
//
//        PersonRecord person = new PersonRecord();
//        person.setFirstName(adddetail.get(6).toString());
//        person.setLastName(adddetail.get(7).toString());
//        person.setDateOfBirth(LocalDate.parse(adddetail.get(8).toString(), formatter));
//        person.setPhoneNumber(adddetail.get(9).toString());
//        person.setSecPhoneNumber(adddetail.get(10).toString());
//        person.setGender(adddetail.get(11).toString());
//        person.setPrimaryLanguage(adddetail.get(12).toString());
//        person.setAddressId(addressID);
//        int personID = Objects.requireNonNull(context.insertInto(PERSON).set(person)
//                .returning(PERSON.PERSON_ID).fetchOne()).getPersonId();
//
//        StaffRecord staff = new StaffRecord();
//        staff.setPersonId(personID);
//        staff.setStaffImage(adddetail.get(13).toString());
//        if(adddetail.get(14).toString().compareTo("NURSE") == 0) staff.setStaffType(StaffStaffType.NURSE);
//        else if(adddetail.get(14).toString().compareTo("DOCTOR") == 0) staff.setStaffType(StaffStaffType.DOCTOR);
//        else if(adddetail.get(14).toString().compareTo("PHARMACIST") == 0)staff.setStaffType(StaffStaffType.PHARMACIST);
//        else return false;
//        staff.setDepartmentId(Integer.parseInt(adddetail.get(15).toString()));
//        staff.setLicenseType(adddetail.get(16).toString());
//        staff.setLicenseNumber(adddetail.get(17).toString());
//        staff.setLicenseExpiryDate(LocalDate.parse(adddetail.get(18).toString(), formatter));
//        staff.setEmpStartDate(LocalDate.parse(adddetail.get(19).toString(), formatter));
//        staff.setEmpEndDate(LocalDate.parse(adddetail.get(20).toString(), formatter));
//        staff.setEmpStatus(adddetail.get(21).toString());
//        staff.setLastInfoUpdate(LocalDateTime.now());
//        int staffID = Objects.requireNonNull(context.insertInto(STAFF).set(staff)
//                .returning(STAFF.STAFF_ID).fetchOne()).getStaffId();
//
//        StaffSpecializationRecord staffspecie = new StaffSpecializationRecord();
//        staffspecie.setStaffId(staffID);
//        staffspecie.setSpecializationId(Integer.parseInt(adddetail.get(22).toString()));
//        context.insertInto(STAFF_SPECIALIZATION).set(staffspecie).execute();
//
        return true;
    }
}
