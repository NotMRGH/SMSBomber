package ir.mrsf.smsbomber.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class API {
    private final String name;
    private final String url;
    private final String payload;
}
