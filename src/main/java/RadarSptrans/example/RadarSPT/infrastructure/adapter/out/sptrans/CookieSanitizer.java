package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import java.net.HttpCookie;
import java.util.List;

final class CookieSanitizer {

    private CookieSanitizer() {
    }

    static String sanitize(String rawCookie) {
        if (rawCookie == null) {
            return "";
        }

        String trimmedCookie = rawCookie.trim();
        if (trimmedCookie.isEmpty()) {
            return "";
        }

        try {
            List<HttpCookie> parsedCookies = HttpCookie.parse(trimmedCookie);
            if (!parsedCookies.isEmpty()) {
                HttpCookie cookie = parsedCookies.get(0);
                if (!cookie.getName().isEmpty()) {
                    return cookie.getName() + "=" + cookie.getValue();
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Fallback to manual sanitization below.
        }

        int delimiterIndex = trimmedCookie.indexOf(';');
        String sanitizedCookie = delimiterIndex >= 0 ? trimmedCookie.substring(0, delimiterIndex) : trimmedCookie;
        return sanitizedCookie.trim();
    }
}
