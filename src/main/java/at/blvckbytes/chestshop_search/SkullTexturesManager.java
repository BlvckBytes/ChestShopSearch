package at.blvckbytes.chestshop_search;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkullTexturesManager {

  private final Map<String, String> texturesByNameLower;
  private final Plugin plugin;
  private final Logger logger;

  public SkullTexturesManager(Plugin plugin, Logger logger) {
    this.texturesByNameLower = new HashMap<>();
    this.plugin = plugin;
    this.logger = logger;
  }

  public void getBase64TexturesIfExist(String ownerName, Consumer<String> handler) {
    var nameLower = ownerName.toLowerCase();
    var cachedTextures = texturesByNameLower.get(nameLower);

    if (cachedTextures != null)
      return;

    var ownerPlayer = Bukkit.getOfflinePlayerIfCached(ownerName);

    if (ownerPlayer == null)
      return;

    var url = "https://sessionserver.mojang.com/session/minecraft/profile/" + ownerPlayer.getUniqueId() + "?unsigned=false";

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try (
        var in = URI.create(url).toURL().openStream();
        var reader = new InputStreamReader(in)
      ) {
        var jsonElement = JsonParser.parseReader(reader);

        if (jsonElement == null || !jsonElement.isJsonObject())
          throw new IllegalStateException("Malformed response");

        var json = jsonElement.getAsJsonObject();
        var properties = json.getAsJsonArray("properties");

        for (JsonElement e : properties) {
          JsonObject prop = e.getAsJsonObject();
          if ("textures".equals(prop.get("name").getAsString())) {
            var value = prop.get("value").getAsString();
            texturesByNameLower.put(nameLower, value);
            handler.accept(value);
            return;
          }
        }

        throw new IllegalStateException("Missing properties.textures");
      } catch (Exception e) {
        logger.log(Level.WARNING, "An error occurred while trying to fetch skull-data for " + ownerName + ": " + e.getMessage());
      }
    });
  }
}
