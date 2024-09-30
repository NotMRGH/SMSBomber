package ir.mrsf.smsbomber.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContentType {
    JSON("application/json"),
    www_form_urlencoded("application/x-www-form-urlencoded");

    private final String string;

    public static ContentType fromString(String string) {
        for (ContentType type : ContentType.values()) {
            if (type.string.equalsIgnoreCase(string)) {
                return type;
            }
        }
        return null;
    }
}
