package com.nkd.medicare.service;
import com.nkd.medicare.domain.DataExcelComplete;
import org.apache.poi.ss.usermodel.Row;

import java.util.Map;

public interface AdminService {
    public Map<String, DataExcelComplete> readexcel(String url);
    public DataExcelComplete addstaff(Row Url) throws Exception;
    public DataExcelComplete delstaff(Row Url);
    public boolean updatestaff(Row Url);
    
}
