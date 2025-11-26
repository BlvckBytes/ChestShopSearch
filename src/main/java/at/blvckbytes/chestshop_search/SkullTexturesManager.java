package at.blvckbytes.chestshop_search;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SkullTexturesManager {

  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

  private final Map<String, UUID> playerIdByNameLower;
  private final Map<String, String> texturesByNameLower;
  private final Plugin plugin;
  private final Logger logger;
  private final boolean hasFloodgate;

  public SkullTexturesManager(Plugin plugin, Logger logger) {
    this.playerIdByNameLower = new HashMap<>();
    this.texturesByNameLower = new HashMap<>();
    this.plugin = plugin;
    this.logger = logger;
    this.hasFloodgate = Bukkit.getServer().getPluginManager().isPluginEnabled("floodgate");

    if (hasFloodgate)
      logger.info("Floodgate detected - will use their APIs to retrieve skin-data.");
  }

  public void getBase64TexturesIfExist(String ownerName, Consumer<String> handler) {
    var nameLower = ownerName.toLowerCase();
    var cachedTextures = texturesByNameLower.get(nameLower);

    if (cachedTextures != null) {
      handler.accept(cachedTextures);
      return;
    }

    if (hasFloodgate) {
      var floodgateApi = FloodgateApi.getInstance();

      if (nameLower.startsWith(floodgateApi.getPlayerPrefix().toLowerCase())) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
          try {
            UUID cachedId;

            if (playerIdByNameLower.containsKey(nameLower))
              cachedId = playerIdByNameLower.get(nameLower);
            else {
              cachedId = floodgateApi.getUuidFor(ownerName).get();
              playerIdByNameLower.put(nameLower, cachedId);
            }

            if (cachedId == null)
              throw new IllegalStateException("Could not resolve UUID");

            var floodgatePlayer = floodgateApi.getPlayer(cachedId);

            if (floodgatePlayer == null)
              throw new IllegalStateException("Could not resolve floodgate-player");

            var value = (String) floodgatePlayer.getProperty("textures");

            if (value == null)
              throw new IllegalStateException("Could not resolve textures-value");

            texturesByNameLower.put(nameLower, value);
            handler.accept(value);
          } catch (Throwable e) {
            logger.log(Level.WARNING, "An error occurred while trying to fetch Bedrock skull-data for " + ownerName + ": " + e.getMessage());
          }
        });

        return;
      }
    }

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        UUID cachedId;

        if (playerIdByNameLower.containsKey(nameLower))
          cachedId = playerIdByNameLower.get(nameLower);
        else {
          var cachedPlayer = Bukkit.getOfflinePlayerIfCached(ownerName);

          if (cachedPlayer != null)
            cachedId = cachedPlayer.getUniqueId();
          else {
            var url = "https://api.mojang.com/users/profiles/minecraft/" + ownerName;

            var request = HttpRequest.newBuilder(URI.create(url)).build();
            var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200)
              throw new IllegalStateException("Non-200 status-code: " + response.statusCode());

            var jsonElement = JsonParser.parseString(response.body());

            if (jsonElement == null || !jsonElement.isJsonObject())
              throw new IllegalStateException("Non-json response");

            var json = jsonElement.getAsJsonObject();

            if (!(json.get("id") instanceof JsonPrimitive idPrimitive))
              throw new IllegalStateException("Missing key \"id\"");

            cachedId = fromDashLessString(idPrimitive.getAsString());
          }
        }

        if (cachedId == null)
          throw new IllegalStateException("Could not resolve UUID");

        var url = "https://sessionserver.mojang.com/session/minecraft/profile/" + cachedId + "?unsigned=false";

        var request = HttpRequest.newBuilder(URI.create(url)).build();
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
          throw new IllegalStateException("Non-200 status-code: " + response.statusCode());

        var jsonElement = JsonParser.parseString(response.body());

        if (jsonElement == null || !jsonElement.isJsonObject())
          throw new IllegalStateException("Non-json response");

        var json = jsonElement.getAsJsonObject();

        if (!(json.get("properties") instanceof JsonArray properties))
          throw new IllegalStateException("Missing key \"properties\" (or is not an array)");

        for (JsonElement propertyElement : properties) {
          if (!(propertyElement instanceof JsonObject property))
            continue;

          if (!(property.get("name") instanceof JsonPrimitive name))
            continue;

          if (!"textures".equals(name.getAsString()))
            continue;

          if (!(property.get("value") instanceof JsonPrimitive value))
            continue;

          var valueString = value.getAsString();
          texturesByNameLower.put(nameLower, valueString);
          handler.accept(valueString);
          return;
        }

        throw new IllegalStateException("Missing properties.textures");
      } catch (Exception e) {
        logger.log(Level.WARNING, "An error occurred while trying to fetch Java skull-data for " + ownerName + ": " + e.getMessage());
      }
    });
  }

  public static UUID fromDashLessString(String input) {
    if (input == null || input.length() != 32)
      throw new IllegalArgumentException("Invalid dash-less UUID: " + input);

    var sb = new StringBuilder(input);
    sb.insert(8, '-');
    sb.insert(13, '-');
    sb.insert(18, '-');
    sb.insert(23, '-');

    return UUID.fromString(sb.toString());
  }
}
