package com.nkd.medicare.service.CCCD;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class check {

    public static void main(String[] args) {
        String Text = "{\"ParsedResults\":[{\"TextOverlay\":{\"Lines\":[],\"HasOverlay\":false,\"Message\":\"Text overlay is not provided as it is not requested\"},\"TextOrientation\":\"0\",\"FileParseExitCode\":1,\"ParsedText\":\"Có giá trị đến: 02/03/2028\\nDate af axpiry\\nCỘNG HÒA XẢ HỘI CHỦ NGHĨA VIỆT NAM\\nĐộc lập - Ty do - Hạnh phúc\\nSOCIALIST REPUBLIC OF VIET NAM\\nIndependence : Freedom - Happiness\\nCĂN CƯỚC CÔNG DÂN\\nCitizen Identity Card\\nSố / No.: 095203000385\\nHọ và tên / Full name:\\nNGÔ TẤN PHÁT\\nNgày sinh / Date of birth: 02/03/2003\\nGiới tính / Sex: Nam Quốc tịch / Nationality: Việt Nam\\nQuê quán / Place of origin:\\nPhước Long, Bạc Liêu\\nNơi thường trủ / Place of residence: Ap Tân Tạo\\nThị trấn Châu Hưng, Vĩnh Lợi, Bạc Liêu\",\"ErrorMessage\":\"\",\"ErrorDetails\":\"\"}],\"OCRExitCode\":1,\"IsErroredOnProcessing\":false,\"ProcessingTimeInMilliseconds\":\"1297\",\"SearchablePDFURL\":\"Searchable PDF not generated as it was not requested.\"}";
        String inputText = Text.replaceAll("\\\\n", " ").replaceAll("\\\\", " ");

        Pattern CCCDPattern = Pattern.compile("Số / No\\.:\\s*(\\d+)");
        Pattern NamePattern = Pattern.compile("Họ và tên / Full name:\\s*(.*?)\\s*Ngày sinh / Date of birth:");
        Pattern BirthdayPattern = Pattern.compile("Ngày sinh / Date of birth:\\s*(\\d{2}/\\d{2}/\\d{4})");
        Pattern SexPattern = Pattern.compile("Giới tính / Sex:\\s*(\\p{L}+)");
        Pattern NationalityPattern = Pattern.compile("Quốc tịch / Nationality:\\s*(\\p{L}+\\s*\\p{L}*)");
        Pattern ResidencePattern = Pattern.compile("Nơi thường trủ / Place of residence:\\s*([\\p{L}\\s,]+)");

        Matcher BirthdayMatcher = BirthdayPattern.matcher(inputText);
        Matcher SexMatcher = SexPattern.matcher(inputText);
        Matcher NationalityMatcher = NationalityPattern.matcher(inputText);
        Matcher ResidenceMatcher = ResidencePattern.matcher(inputText);
        Matcher CCCDMatcher = CCCDPattern.matcher(inputText);
        Matcher NameMatcher = NamePattern.matcher(inputText);

        if (NameMatcher.find() && CCCDMatcher.find() && BirthdayMatcher.find() && SexMatcher.find() && ResidenceMatcher.find()) {

            String cccd = CCCDMatcher.group(1);
            String name = NameMatcher.group(1);
            String birthday = BirthdayMatcher.group(1);
            String sex = SexMatcher.group(1);
            String Residence = ResidenceMatcher.group(1);

            System.out.println("So CCCD: " + cccd);
            System.out.println("Ten: " + name);
            System.out.println("Ngay sinh: " + birthday);
            System.out.println("Gioi tinh: " + sex);
            System.out.println("Dia chi: " + Residence);
        }
    }
}
