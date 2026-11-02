package dev.alek.pinktools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pinktools.json");
    private static Config instance;

    // nicki
    public boolean nickHighlight = true;
    public int nickColor = 0xFF69B4;
    public List<String> nicks = new ArrayList<>();

    // trajektorie
    public boolean trajectories = true;
    public int ownColor = 0xFF69B4;
    public int otherColor = 0x3C8CFF;

    // trafienie
    public boolean hitHighlight = true;
    public int hitColor = 0xFF5555;

    // fastplace
    public boolean fastPlace = false;
    public int cps = 10; // 1..15

    public static Config get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    instance = GSON.fromJson(r, Config.class);
                }
            }
        } catch (Exception ignored) {
        }
        if (instance == null) instance = new Config();
        if (instance.nicks == null) instance.nicks = new ArrayList<>();
        instance.cps = Math.max(1, Math.min(15, instance.cps));
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(get(), w);
        } catch (Exception ignored) {
        }
    }

    public boolean isFriend(String name) {
        for (String n : nicks) {
            if (n.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
