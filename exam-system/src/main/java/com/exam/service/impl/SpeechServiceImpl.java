package com.exam.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.exam.service.SpeechService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Proxy;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SpeechServiceImpl implements SpeechService {

    @Value("${baidu.speech.app-id:}")
    private String appId;

    @Value("${baidu.speech.api-key:}")
    private String apiKey;

    @Value("${baidu.speech.secret-key:}")
    private String secretKey;

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String ASR_URL = "https://vop.baidu.com/server_api";

    private volatile String accessToken;
    private volatile long tokenExpireTime;

    /** 缓存的 SSLSocketFactory，信任所有证书，兼容 VPN/代理环境 */
    private static final SSLSocketFactory TRUST_ALL_SSL_FACTORY;
    static {
        try {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                @Override public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                @Override public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }};
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAll, new SecureRandom());
            TRUST_ALL_SSL_FACTORY = sc.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("SSL 初始化失败", e);
        }
    }

    /** 带重试的 HTTP POST 请求，绕过系统代理 + 信任所有证书 */
    private String safePost(String url, String body, int timeoutMs) {
        int maxRetries = 3;
        Exception lastEx = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                HttpRequest req = HttpRequest.post(url)
                        .setProxy(Proxy.NO_PROXY)
                        .setSSLSocketFactory(TRUST_ALL_SSL_FACTORY)
                        .setHostnameVerifier((h, s) -> true)
                        .timeout(timeoutMs);
                if (body != null) {
                    req.contentType("application/json").body(body);
                }
                return req.execute().body();
            } catch (Exception e) {
                lastEx = e;
                log.warn("HTTP 请求失败(第{}次): {} - {}", i + 1, url, e.getMessage());
                if (i < maxRetries - 1) {
                    try { Thread.sleep(500L * (i + 1)); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }
            }
        }
        throw new RuntimeException("HTTP 请求最终失败(已重试" + maxRetries + "次): " + lastEx.getMessage(), lastEx);
    }

    @Override
    public String recognize(MultipartFile audioFile) {
        if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new RuntimeException("百度语音识别未配置，请在 application-dev.yml 中配置 baidu.speech.api-key 和 baidu.speech.secret-key");
        }

        try {
            String originalName = audioFile.getOriginalFilename();
            log.info("音频文件: name={}, contentType={}, size={}", originalName, audioFile.getContentType(), audioFile.getSize());

            // 前端已将音频转为标准 WAV（16kHz, 16bit, mono），去掉44字节WAV头即为PCM
            byte[] wavBytes = audioFile.getBytes();
            byte[] pcmData;
            if (wavBytes.length > 44) {
                pcmData = Arrays.copyOfRange(wavBytes, 44, wavBytes.length);
            } else {
                throw new RuntimeException("音频文件过小或格式不正确");
            }

            // 获取百度 access_token
            String token = getAccessToken();

            // 调用百度语音识别 API
            return callBaiduAsr(pcmData, token);
        } catch (Exception e) {
            log.error("语音识别失败", e);
            throw new RuntimeException("语音识别失败: " + e.getMessage());
        }
    }

    /**
     * 获取百度 access_token（带缓存）
     */
    private synchronized String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        String url = TOKEN_URL + "?grant_type=client_credentials&client_id=" + apiKey + "&client_secret=" + secretKey;
        String response = safePost(url, null, 10000);
        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("access_token")) {
            accessToken = json.getStr("access_token");
            int expiresIn = json.getInt("expires_in", 2592000);
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 600) * 1000L;
            return accessToken;
        } else {
            throw new RuntimeException("获取百度 access_token 失败: " + response);
        }
    }

    /**
     * 调用百度语音识别 REST API
     */
    private String callBaiduAsr(byte[] pcmData, String token) {
        String speech = Base64.getEncoder().encodeToString(pcmData);

        Map<String, Object> params = new HashMap<>();
        params.put("format", "pcm");
        params.put("rate", 16000);
        params.put("channel", 1);
        params.put("cuid", "exam-system-" + (appId.isEmpty() ? "default" : appId));
        params.put("token", token);
        params.put("speech", speech);
        params.put("len", pcmData.length);
        params.put("dev_pid", 1537); // 普通话+英文混合模型

        String response = safePost(ASR_URL, JSONUtil.toJsonStr(params), 15000);
        log.info("百度语音识别响应: {}", response);

        JSONObject json = JSONUtil.parseObj(response);
        int errNo = json.getInt("err_no", -1);
        if (errNo == 0) {
            return json.getJSONArray("result").getStr(0, "").trim();
        } else {
            String errMsg = json.getStr("err_msg", "未知错误");
            throw new RuntimeException("百度语音识别错误[" + errNo + "]: " + errMsg);
        }
    }
}
