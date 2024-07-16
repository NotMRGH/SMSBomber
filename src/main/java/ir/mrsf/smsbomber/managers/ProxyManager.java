package ir.mrsf.smsbomber.managers;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ProxyManager {
    private final List<Proxy> proxies;
    private int proxyIndex;

    public ProxyManager() {
        this.proxies = new ArrayList<>();
        this.proxyIndex = 0;
    }

    public synchronized Proxy getNextProxy() {
        if (proxies.isEmpty()) {
            return Proxy.NO_PROXY;
        }
        final Proxy proxy = proxies.get(proxyIndex);
        proxyIndex = (proxyIndex + 1) % proxies.size();
        return proxy;
    }

    public void load() {
        try {
            final URL url = new URL("https://api.proxyscrape.com/v3/free-proxy-list/get?request=displayproxies&protocol=http&proxy_format=ipport&format=text&timeout=3000");
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    final String[] parts = inputLine.split(":");
                    if (parts.length != 2) continue;
                    final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
                    final HttpURLConnection connection = (HttpURLConnection) new URL("http://www.google.com").openConnection(proxy);
                    connection.setConnectTimeout(3000);
                    connection.connect();
                    if (connection.getResponseCode() == 200) {
                        proxies.add(proxy);
                    }
                }
            } catch (IOException ignored) {
            }
        } catch (IOException ignored) {
            System.out.println("Error loading proxy");
        }
    }
}
