package com.aiagent.workflow.service.executor.impl;

import com.aiagent.workflow.entity.WorkflowNode;
import com.aiagent.workflow.service.executor.NodeExecutor;
import com.aiagent.workflow.service.executor.WorkflowExecutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP节点执行器
 * 实现了NodeExecutor接口，用于处理HTTP请求节点
 * 迁移自 CodeHubot (支持重试、SSL验证开关、变量替换)
 */
@Component
public class HttpNodeExecutor implements NodeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(HttpNodeExecutor.class);

    @Autowired
    private RestTemplate defaultRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getSupportedNodeType() {
        return "http";
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(WorkflowNode node, WorkflowExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("开始执行HTTP节点: {}", node.getName());
                
                // 1. 获取配置参数
                Map<String, Object> data = node.getData();
                if (data == null) throw new IllegalArgumentException("HTTP节点配置数据不能为空");

                String urlRaw = (String) data.get("url");
                if (urlRaw == null || urlRaw.isEmpty()) throw new IllegalArgumentException("HTTP请求URL不能为空");
                
                String methodStr = (String) data.getOrDefault("method", "GET");
                HttpMethod method = HttpMethod.valueOf(methodStr.toUpperCase());
                
                Map<String, Object> headersMap = (Map<String, Object>) data.get("headers");
                Object bodyRaw = data.get("body");
                
                Integer timeout = (Integer) data.getOrDefault("timeout", 10000); // 默认10秒
                Integer retryCount = (Integer) data.getOrDefault("retryCount", 0);
                Boolean validateSSL = (Boolean) data.getOrDefault("validateSSL", true);
                
                // 2. 变量替换
                String url = context.replaceVariables(urlRaw);
                logger.debug("URL: {}", url);

                HttpHeaders headers = new HttpHeaders();
                if (headersMap != null) {
                    for (Map.Entry<String, Object> entry : headersMap.entrySet()) {
                        String value = entry.getValue() != null ? entry.getValue().toString() : "";
                        headers.add(entry.getKey(), context.replaceVariables(value));
                    }
                }
                if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }

                String body = null;
                if (bodyRaw != null) {
                    if (bodyRaw instanceof String) {
                        body = context.replaceVariables((String) bodyRaw);
                    } else {
                        // 如果body是对象/Map，先转JSON再替换变量
                        // 注意：这里简单实现为转字符串后替换。更精细的做法是递归替换Map中的值。
                        // CodeHubot Python代码做了递归替换。为了简单且强大，
                        // 我们这里先转JSON字符串，然后对整体进行变量替换，
                        // 这样能支持 {input.obj} 替换为整个JSON片段，也能支持 {"key": "{input.val}"}。
                        String jsonBody = objectMapper.writeValueAsString(bodyRaw);
                        body = context.replaceVariables(jsonBody);
                    }
                }

                HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

                // 3. 准备 RestTemplate (处理 SSL 和 超时)
                RestTemplate restTemplateToUse = defaultRestTemplate;
                if (!validateSSL) {
                    restTemplateToUse = createInsecureRestTemplate(timeout);
                } else if (timeout != null) {
                    // 如果需要自定义超时但SSL验证正常，也需要新factory
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(timeout);
                    factory.setReadTimeout(timeout);
                    restTemplateToUse = new RestTemplate(factory);
                }

                // 4. 执行请求 (带重试)
                ResponseEntity<String> responseEntity = null;
                Exception lastException = null;

                for (int i = 0; i <= retryCount; i++) {
                    try {
                        if (i > 0) {
                            logger.info("HTTP请求重试 {}/{}", i, retryCount);
                            Thread.sleep(1000); // 重试等待
                        }
                        responseEntity = restTemplateToUse.exchange(url, method, requestEntity, String.class);
                        break; // 成功则跳出循环
                    } catch (Exception e) {
                        lastException = e;
                        logger.warn("HTTP请求尝试 {} 失败: {}", i + 1, e.getMessage());
                    }
                }

                if (responseEntity == null) {
                    throw new RuntimeException("HTTP请求失败(重试" + retryCount + "次): " + (lastException != null ? lastException.getMessage() : "未知错误"), lastException);
                }

                // 5. 构建输出结果 (对齐 CodeHubot)
                Map<String, Object> result = new HashMap<>();
                int statusCode = responseEntity.getStatusCodeValue();
                String responseBodyStr = responseEntity.getBody();
                
                Object responseBodyObj = responseBodyStr;
                try {
                   // 尝试解析响应体为 JSON 对象，方便后续节点使用 {node.data.field} 访问
                   if (responseBodyStr != null && (responseBodyStr.startsWith("{") || responseBodyStr.startsWith("["))) {
                       responseBodyObj = objectMapper.readValue(responseBodyStr, Object.class);
                   }
                } catch (Exception ignored) {}

                result.put("status_code", statusCode); // Python: status_code
                result.put("status", statusCode);      // Alias
                result.put("body", responseBodyObj);   // Python: body (content)
                result.put("data", responseBodyObj);   // Python: data (alias)
                result.put("success", statusCode >= 200 && statusCode < 300);
                
                // Headers output
                Map<String, Object> headersOut = new HashMap<>();
                for (Map.Entry<String, List<String>> entry : responseEntity.getHeaders().entrySet()) {
                    headersOut.put(entry.getKey(), entry.getValue().isEmpty() ? "" : entry.getValue().get(0));
                }
                result.put("headers", headersOut);

                logger.info("HTTP节点执行成功: Status={}", statusCode);
                return result;

            } catch (Exception e) {
                logger.error("HTTP节点执行异常", e);
                // 即使失败，也尽量返回结构化错误信息，而不是直接抛出异常导致工作流中断(取决于引擎策略)
                // 但 NodeExecutor 接口定义是抛出异常还是返回包含 error 的 map? 
                // 原实现是 throw RuntimeException，维持原样让引擎处理
                throw new RuntimeException(e);
            }
        });
    }

    private RestTemplate createInsecureRestTemplate(int timeout) {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };
            factory.setConnectTimeout(timeout);
            factory.setReadTimeout(timeout);
            
            return new RestTemplate(factory);
        } catch (Exception e) {
            logger.error("创建非安全 RestTemplate 失败", e);
            return new RestTemplate();
        }
    }
}