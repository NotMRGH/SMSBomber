package ir.mrsf.smsbomber.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WebUtil {

    public static boolean isWordPressSite(int timeout, String url) {
        try {
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }

            final Document document = Jsoup.connect(url)
                    .timeout((int) TimeUnit.SECONDS.toMillis(timeout))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();
            final boolean hasWpContent = document.html().contains("/wp-content/") ||
                                         document.html().contains("/wp-includes/");

            final boolean hasWpGenerator = document.select("meta[name=generator]").stream()
                    .anyMatch(element -> element.attr("content").toLowerCase().contains("wordpress"));

            final boolean hasWpApi = !document.select("link[rel=https://api.w.org/]").isEmpty();

            final boolean hasWpCookies = document.connection().response().cookies().keySet().stream()
                    .anyMatch(cookie -> cookie.startsWith("wp-") || cookie.startsWith("wordpress"));

            return hasWpContent || hasWpGenerator || hasWpApi || hasWpCookies;

        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean usesPhoneLogin(int timeout, String domain, String[] keywords) {
        try {
            final String baseUrl = domain.startsWith("http") ? domain : "https://" + domain;
            final Document homePage = Jsoup.connect(baseUrl)
                    .timeout((int) TimeUnit.SECONDS.toMillis(timeout))
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                               " Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            final Elements links = homePage.select("a[href]");
            final Set<String> checkedUrls = new HashSet<>();

            for (Element link : links) {
                final String href = link.absUrl("href");
                if (href.startsWith(baseUrl) && !checkedUrls.contains(href)) {
                    checkedUrls.add(href);

                    final Document doc = Jsoup.connect(href)
                            .timeout((int) TimeUnit.SECONDS.toMillis(timeout))
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                       "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .get();

                    final String pageText = doc.text().toLowerCase();

                    for (String keyword : keywords) {
                        if (!pageText.contains(keyword)) continue;
                        return true;
                    }
                }
            }

        } catch (IOException ignored) {
            return false;
        }
        return false;
    }

} 