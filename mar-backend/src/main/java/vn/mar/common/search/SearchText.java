package vn.mar.common.search;

import java.util.Locale;
import org.springframework.util.StringUtils;

public final class SearchText {

    private SearchText() {
    }

    public static String textOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public static String keyword(String value) {
        String text = textOrNull(value);
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

    public static String upperOrNull(String value) {
        String text = textOrNull(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }
}
