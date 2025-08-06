package com.wechatDomainCheck.check;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * 第一种域名检查方法
 *
 * @author Ricky Li
 * @version 1.0
 * @date 8/6/25 10:03 AM
 */
public class Check1 {

    public static void main(String[] args) {
        // 填入你自己的域名进行测试
        String urlToCheck = "https://XXXXXXXX";
        String result = checkWeChatDomain(urlToCheck);
        System.out.println(result);
    }

    private static String checkWeChatDomain(String url) {
        JsonObject result = new JsonObject();

        if (url == null || url.isEmpty()) {
            result.addProperty("code", 202);
            result.addProperty("msg", "请传入Url");
            return result.toString();
        }

        try {
            // Call official API
            String checkUrl = "https://cgi.urlsec.qq.com/index.php?m=url&a=validUrl&url=" + URLEncoder.encode(url, "UTF-8");

            HttpURLConnection connection = (HttpURLConnection) new URL(checkUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Parse JSON response
                JsonObject jsonResponse = new JsonParser().parse(response.toString()).getAsJsonObject();
                String dataMsg = jsonResponse.get("data").getAsString();

                if ("ok".equals(dataMsg)) {
                    // Domain is blocked
                    result.addProperty("code", 202);
                    result.addProperty("msg", "域名被封");
                } else {
                    // Domain is normal
                    result.addProperty("code", 200);
                    result.addProperty("msg", dataMsg);
                }
            } else {
                result.addProperty("code", responseCode);
                result.addProperty("msg", "API请求失败");
            }
        } catch (Exception e) {
            result.addProperty("code", 500);
            result.addProperty("msg", "检查过程中出现错误: " + e.getMessage());
        }

        return result.toString();
    }
}
