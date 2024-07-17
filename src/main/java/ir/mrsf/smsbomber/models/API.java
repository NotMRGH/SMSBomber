package ir.mrsf.smsbomber.models;

import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class API {
    private final String name;
    private final String url;
    private final String countryCode;
    private final boolean withOutZero;
    private final JsonElement payload;
}
