package ir.mrsf.smsbomber.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ir.mrsf.smsbomber.enums.Method;
import ir.mrsf.smsbomber.models.API;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ConfigManager {
    private final List<API> apiList;

    public ConfigManager() {
        this.apiList = new ArrayList<>();
        this.load();
    }

    public void load() {
        try {
            this.apiList.clear();
            final File apiFile = new File("config.json");
            if (!apiFile.exists()) {
                if (!apiFile.createNewFile()) {
                    System.out.println("Could not create config file");
                }
                return;
            }
            try (final FileReader reader = new FileReader(apiFile)) {
                final JsonElement jsonElement = JsonParser.parseReader(reader);
                if (!(jsonElement instanceof JsonArray jsonArray)) {
                    System.out.println("Could not parse config file");
                    return;
                }
                int i = 0;
                for (JsonElement element : jsonArray) {
                    if (!(element instanceof JsonObject jsonObject)) {
                        System.out.println("Invalid config file");
                        continue;
                    }
                    boolean withOutZero = false;
                    if (jsonObject.has("withOutZero")) {
                        withOutZero = jsonObject.get("withOutZero").getAsBoolean();
                    }
                    final API api = new API(
                            jsonObject.get("name").getAsString(),
                            jsonObject.get("url").getAsString(),
                            withOutZero,
                            Method.valueOf(jsonObject.get("method").getAsString()),
                            jsonObject.get("repeat").getAsInt(),
                            jsonObject.get("payload")
                    );
                    i++;
                    this.apiList.add(api);
                }
                System.out.println("Loaded " + i + " API(s)");
            }
        } catch (IOException ignored) {
            throw new RuntimeException("Could not load config file");
        }
    }
}
