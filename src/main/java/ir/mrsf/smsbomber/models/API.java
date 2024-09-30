package ir.mrsf.smsbomber.models;

import com.google.gson.JsonElement;
import ir.mrsf.smsbomber.enums.Method;

public record API(String name, String url, boolean withOutZero, String forceContentType,
                  Method method, int repeat, JsonElement payload) {
}
