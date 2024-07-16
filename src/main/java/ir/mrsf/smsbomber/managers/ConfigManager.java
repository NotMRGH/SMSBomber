package ir.mrsf.smsbomber.managers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ir.mrsf.smsbomber.models.API;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    @Getter
    private final List<API> apiList;
    private final Gson gson;

    public ConfigManager() {
        this.apiList = new ArrayList<>();
        this.gson = new Gson();
        this.load();
    }

    @SneakyThrows
    public void load() {
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
            if (!(jsonElement instanceof JsonArray jsonArray)) return;
            for (JsonElement element : jsonArray) {
                this.apiList.add(this.gson.fromJson(element, API.class));
            }
        }
    }
}
