package com.nkd.medicare.service.cccd;

import com.nkd.medicare.exception.ApiException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private String gender;
    private String city;
    private String district;
    private String street;
    private String houseNumber;

    public void scan(String imageURL){
        try {
            String apiKey = "K89360669988957";
            URL url = URI.create("https://api.ocr.space/parse/image").toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("apikey", apiKey);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

            writer.append("url=").append(java.net.URLEncoder.encode(imageURL, StandardCharsets.UTF_8))
                    .append("&language=eng")
                    .append("&OCREngine=2");
            writer.flush();
            outputStream.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputStream = reader.readLine();

            check(inputStream);
            reader.close();
        } catch (Exception e) {
            throw new ApiException("Error when connect to internet, please recheck your connection");
        }
    }

    private void check(String CCCDInfo){
        String inputText = CCCDInfo.replaceAll("\\\\n", " ").replaceAll("\\\\", " ");
        Pattern CCCDPattern = Pattern.compile( "Số.*?:\\s*(.*?)\\s*Họ");
        Pattern namePattern = Pattern.compile("name:\\s*(.*?)\\s*Ngày");
        Pattern birthdayPattern = Pattern.compile("birth:\\s*(\\d{2}/\\d{2}/\\d{4})");
        Pattern genderPattern = Pattern.compile("Sex:\\s*(\\p{L}+)");
        Pattern residencePattern = Pattern.compile("residence:(.*?)(?:\\s*,\\s*\"ErrorMessage|\",\"ErrorMessage\")");

        Matcher birthdayMatcher = birthdayPattern.matcher(inputText);
        Matcher genderMatcher = genderPattern.matcher(inputText);
        Matcher residenceMatcher = residencePattern.matcher(inputText);
        Matcher CCCDMatcher = CCCDPattern.matcher(inputText);
        Matcher nameMatcher = namePattern.matcher(inputText);

        if (CCCDMatcher.find() && nameMatcher.find() && residenceMatcher.find() && birthdayMatcher.find() && genderMatcher.find()) {
            this.cccd = CCCDMatcher.group(1);

            String[] name = nameMatcher.group(1).split(" ");
            this.firstname = name[name.length-1];
            this.lastname = String.join(" ", java.util.Arrays.copyOf(name, name.length - 1));

            String var1 = birthdayMatcher.group(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            this.birthday = LocalDate.parse(var1, formatter);

            this.gender = genderMatcher.group(1);

            String residence = residenceMatcher.group(1);
            this.city = residence.substring(residence.lastIndexOf(",") + 1).trim();
            this.district = residence.split(",")[1].trim();
            this.street = residence.split(",")[0].trim();
            this.houseNumber = this.street.split(" ")[0].trim();
            boolean containsDigit = this.houseNumber.chars().anyMatch(Character::isDigit);

            if (!containsDigit)
                this.houseNumber = null;
            else
                this.street = this.street.replace(this.houseNumber +" ","");
        }
        else throw new ApiException("Image of identify card are not clear or not in correct format, sent another image to recheck");
    }
}