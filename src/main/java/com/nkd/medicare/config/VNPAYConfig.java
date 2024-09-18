package com.nkd.medicare.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.nkd.medicare.utils.VNPayUtil;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
@Configuration
public class VNPAYConfig {
    @Getter
    @Value("${PAY_URL}")
    private String vnp_PayUrl;
    @Value("${RETURN_URL}")
    private String vnp_ReturnUrl;
    @Value("${TMN_CODE}")
    private String vnp_TmnCode ;
    @Getter
    @Value("${SECRET_KEY}")
    private String secretKey;
    @Value("${VERSION}")
    private String vnp_Version;
    @Value("${COMMAND}")
    private String vnp_Command;
    @Value("${ORDER_TYPE}")
    private String orderType;

    public Map<String, String> getVNPayConfig(String appointmentID) {
        Map<String, String> vnpParamsMap = new HashMap<>();
        vnpParamsMap.put("vnp_Version", this.vnp_Version);
        vnpParamsMap.put("vnp_Command", this.vnp_Command);
        vnpParamsMap.put("vnp_TmnCode", this.vnp_TmnCode);
        vnpParamsMap.put("vnp_CurrCode", "VND");
        vnpParamsMap.put("vnp_TxnRef", appointmentID );
        vnpParamsMap.put("vnp_OrderInfo", "Thanh toan hoa don dat lich kham benh");
        vnpParamsMap.put("vnp_OrderType", this.orderType);
        vnpParamsMap.put("vnp_Locale", "vn");
        vnpParamsMap.put("vnp_ReturnUrl", this.vnp_ReturnUrl);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnpParamsMap.put("vnp_CreateDate", vnpCreateDate);
        calendar.add(Calendar.MONTH, 1);
        String vnp_ExpireDate = formatter.format(calendar.getTime());
        vnpParamsMap.put("vnp_ExpireDate", vnp_ExpireDate);
        return vnpParamsMap;
    }
}
