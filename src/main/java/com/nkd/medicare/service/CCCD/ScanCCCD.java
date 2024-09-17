package com.nkd.medicare.service.CCCD;

import com.nkd.medicare.exception.ApiException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ScanCCCD {
    private String cccd;
    private String firstname;
    private String lastname;
    private LocalDate birthday;
    private String sex;
    private String City;
    private String District;
    private String Street;
    private String Housenumber;
    public void scan(String urlimage){
        try {
            String apiKey = "K89360669988957";  // Replace with your API key
            String imageUrl = urlimage;  // Replace with your image URL
            URL url = new URL("https://api.ocr.space/parse/image");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);

            writer.append("url=").append(java.net.URLEncoder.encode(imageUrl, "UTF-8"))
                    .append("&language=eng")
                    .append("&OCREngine=2");
            writer.flush();
            os.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            line = in.readLine();
            check(line);
            in.close();
        } catch (Exception e) {
            throw new ApiException("Error when connect to internet, please recheck your connection");
        }
    }
    public void check(String inforCCCD){
        String inputText = inforCCCD.replaceAll("\\\\n", " ").replaceAll("\\\\", " ");
        System.out.println(inputText);
        Pattern CCCDPattern = Pattern.compile( "Số.*?:\\s*(.*?)\\s*Họ");
        Pattern NamePattern = Pattern.compile("name:\\s*(.*?)\\s*Ngày");
        Pattern BirthdayPattern = Pattern.compile("birth:\\s*(\\d{2}/\\d{2}/\\d{4})");
        Pattern SexPattern = Pattern.compile("Sex:\\s*(\\p{L}+)");
        Pattern ResidencePattern = Pattern.compile("residence:(.*?)(?:\\s*,\\s*\"ErrorMessage|\",\"ErrorMessage\")");

        Matcher BirthdayMatcher = BirthdayPattern.matcher(inputText);
        Matcher SexMatcher = SexPattern.matcher(inputText);
        Matcher ResidenceMatcher = ResidencePattern.matcher(inputText);
        Matcher CCCDMatcher = CCCDPattern.matcher(inputText);
        Matcher NameMatcher = NamePattern.matcher(inputText);


        if (CCCDMatcher.find() && NameMatcher.find() && ResidenceMatcher.find() && BirthdayMatcher.find() && SexMatcher.find()) {
            this.cccd = CCCDMatcher.group(1);

            String[] name = NameMatcher.group(1).split(" ");
            this.lastname = name[name.length-1];
            this.firstname = String.join(" ", java.util.Arrays.copyOf(name, name.length - 1));

            String birthdayfake = BirthdayMatcher.group(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            this.birthday = LocalDate.parse(birthdayfake, formatter);

            this.sex = SexMatcher.group(1);

            String Residence = ResidenceMatcher.group(1);
            this.City = Residence.substring(Residence.lastIndexOf(",") + 1).trim();
            this.District = Residence.split(",")[1].trim();
            this.Street = Residence.split(",")[0].trim();
            this.Housenumber = this.Street.split(" ")[0].trim();
            boolean containsDigit = this.Housenumber.chars().anyMatch(Character::isDigit);
            if (!containsDigit) this.Housenumber = null;
            else this.Street = this.Street.replace(this.Housenumber+" ","");

            System.out.println(this.cccd);
            System.out.println(this.firstname);
            System.out.println(this.lastname);
            System.out.println(this.birthday);
            System.out.println(this.sex);
            System.out.println(this.City);
            System.out.println(this.District);
            System.out.println(this.Street);
            System.out.println(this.Housenumber);
        }
        else throw new ApiException("Image of identify card are not clear or not in correct format, sent another image to recheck");
    }
}

