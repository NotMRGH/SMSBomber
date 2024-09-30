package ir.mrsf.smsbomber.managers;

import com.google.gson.JsonObject;
import ir.mrsf.smsbomber.SMSBomber;
import lombok.Getter;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public void loadFile() {
        final File proxyFile = new File("proxy.txt");
        if (!proxyFile.exists()) {
            System.out.println("Your proxy file does not exist, please create proxy.txt in program folder");
            return;
        }
        final Set<String> uniqueProxies = new HashSet<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(proxyFile))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && uniqueProxies.add(line)) {
                    this.testProxy(line);
                }
            }
        } catch (IOException ignored) {
        }
    }


    public void loadAuto() {
        try {
            final URL url = new URL("https://api.proxyscrape.com/v3/free-proxy-list/get?request=displayproxies" +
                    "&proxy_format=protocolipport&format=text");
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    this.testProxy(line);
                }
            } catch (IOException ignored) {
            }
        } catch (IOException ignored) {
            System.out.println("Error get proxy list from server");
        }
    }

    public void proxyInfo() {
        if (this.proxies.isEmpty()) {
            System.out.println("No working proxies available.");
            return;
        }
        System.out.println(this.proxies.size() + " Proxy loaded");
    }

    private void testProxy(String proxyString) {
        try {
            final Proxy proxy = getProxyType(proxyString);
            final HttpURLConnection connection = (HttpURLConnection)
                    new URL("http://httpbin.org/ip").openConnection(proxy);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.connect();
            if (connection.getResponseCode() == 200) {
                final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                final StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                final String origin = SMSBomber.getSmsBomber().getGson().fromJson(content.toString(), JsonObject.class)
                        .get("origin").getAsString();
                System.out.println("Successfully added new proxy with IP: " + origin);
                proxies.add(proxy);
            }
        } catch (IOException ignored) {
        }
    }

    private Proxy getProxyType(String proxyString) {
        final String[] parts = proxyString.split(":");
        if (parts.length < 2) throw new RuntimeException("Invalid proxy format");
        final Proxy proxy;
        if (parts.length == 3) {
            final String type = parts[0].toLowerCase();
            switch (type) {
                case "http" -> proxy = new Proxy(Proxy.Type.HTTP,
                        new InetSocketAddress(parts[1].replace("//", ""), Integer.parseInt(parts[2])));
                case "socks4", "socks5", "socks" -> proxy = new Proxy(Proxy.Type.SOCKS,
                        new InetSocketAddress(parts[1].replace("//", ""), Integer.parseInt(parts[2])));
                default -> throw new RuntimeException("Invalid proxy format");
            }
        } else {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
        }
        return proxy;
    }
}
