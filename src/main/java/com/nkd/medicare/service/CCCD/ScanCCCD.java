package com.nkd.medicare.service.CCCD;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class ScanCCCD {

    public static void main(String[] args) {
        try {
            String apiKey = "K89360669988957";  // Replace with your API key
            String imagePath = "E:\\328cc0e1aab10cef55a0.jpg";  // Image file path
            URL url = new URL("https://api.ocr.space/parse/image");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", apiKey);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);

            File imageFile = new File(imagePath);
            writer.append("--*****\r\nContent-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(imageFile.getName()).append("\"\r\n\r\n").flush();
            Files.copy(imageFile.toPath(), os);
            os.flush();

            // Add API key, language (Vietnamese), and engine (Engine 2)
            writer.append("\r\n--*****\r\nContent-Disposition: form-data; name=\"apikey\"\r\n\r\n")
                    .append(apiKey).append("\r\n");
            writer.append("--*****\r\nContent-Disposition: form-data; name=\"language\"\r\n\r\neng\r\n");
            writer.append("--*****\r\nContent-Disposition: form-data; name=\"OCREngine\"\r\n\r\n2\r\n--*****--\r\n");
            writer.flush();

            // Read and print response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.print(line);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

