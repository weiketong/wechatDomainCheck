package com.wechatDomainCheck.check;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * 第二种域名检查方法
 *
 * @author Ricky Li
 * @version 1.0
 * @date 8/6/25 10:11 AM
 */
public class Check2 {

    public static void main(String[] args) {
        // 替换为实际的token和cookie
        String token = "XXXXXXXX";
        String cookie = "slave_sid=XXXXXXXX; slave_user=XXXXXXXX;";

        // 获取要检测的URL（实际应用中可从请求参数获取）
        String url = args.length > 0 ? args[0] : "https://www.baidu.com/";

        Check2 checker = new Check2(token, cookie);

        if (!checker.setUrl(url)) {
            JSONObject errorResult = new JSONObject();
            errorResult.put("code", -1);
            errorResult.put("msg", "请输入要检测的网址");
            System.out.println(errorResult.toJSONString());
            return;
        }

        JSONObject result = checker.check();
        System.out.println(result.toJSONString());
    }

    private final String token;
    private final String cookie;
    private final String userAgent;
    private final String referer;
    private String checkUrl;

    private Check2(String token, String cookie) {
        this.token = token;
        this.cookie = cookie;
        this.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36";
        this.referer = "https://mp.weixin.qq.com/";
    }

    private boolean setUrl(String url) {
        String trimmedUrl = url.trim();
        if (trimmedUrl.isEmpty()) {
            return false;
        }
        try {
            this.checkUrl = URLEncoder.encode(trimmedUrl, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private JSONObject check() {
        JSONObject result = new JSONObject();

        if (checkUrl == null || checkUrl.isEmpty()) {
            result.put("code", -1);
            result.put("msg", "未设置检测URL");
            return result;
        }

        // 生成随机数
        Random random = new Random();
        double randomValue = 0.1 + (0.9 * random.nextDouble());
        String randomStr = String.format("0.%010d", (long) (randomValue * 10000000000L));

        // 构建API URL
        String apiUrl = String.format(
                "https://mp.weixin.qq.com/cgi-bin/operate_appmsg?sub=check_sourceurl" +
                        "&token=%s&lang=zh_CN&f=json&ajax=1" +
                        "&random=%s&sourceurl=%s",
                token, randomStr, checkUrl
        );

        // 创建HTTP客户端
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(apiUrl);

            // 设置请求头
            httpGet.setHeader("Host", "mp.weixin.qq.com");
            httpGet.setHeader("Connection", "keep-alive");
            httpGet.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            httpGet.setHeader("User-Agent", userAgent);
            httpGet.setHeader("Referer", referer);
            httpGet.setHeader("X-Requested-With", "XMLHttpRequest");
            httpGet.setHeader("Cookie", cookie);

            // 执行请求
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    JSONObject json = JSONObject.parseObject(responseBody);

                    // 解析响应结果
                    JSONObject baseResp = json.getJSONObject("base_resp");
                    int ret = baseResp.getIntValue("ret");
                    String errMsg = baseResp.getString("err_msg");

                    if (ret == 0) {
                        result.put("code", 0);
                        result.put("msg", "网址或域名正常");
                        result.put("err_msg", null);
                    } else {
                        result.put("code", -3);
                        result.put("msg", "网址或域名存在风险，已被微信封禁拦截！");
                        result.put("err_msg", errMsg);
                    }
                } else {
                    result.put("code", -2);
                    result.put("msg", "请求失败");
                    result.put("error", "响应内容为空");
                }
            }
        } catch (IOException e) {
            result.put("code", -2);
            result.put("msg", "请求失败");
            result.put("error", e.getMessage());
        }

        return result;
    }
}
