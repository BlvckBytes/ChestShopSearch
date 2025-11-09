package at.blvckbytes.chestshop_search;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UidScopedKeyValueStore {

  public static final String KEY_QUERY_LANGUAGE = "query-language";

  private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().create();

  private final File dataFile;
  private final Logger logger;

  private final Map<UUID, Map<String, String>> namedStampsByUid;
  private boolean isDataDirty;

  public UidScopedKeyValueStore(File dataFile, Logger logger) {
    this.dataFile = dataFile;
    this.logger = logger;
    this.namedStampsByUid = new HashMap<>();

    loadFromDisk();
  }

  public void write(UUID id, String name, String value) {
    isDataDirty = true;

    namedStampsByUid
      .computeIfAbsent(id, k -> new HashMap<>())
      .put(name.toLowerCase(), value);
  }

  /**
   * @return -1 if non-existent; value >= 0 otherwise
   */
  public @Nullable String read(UUID id, String name) {
    var stampByName = namedStampsByUid.get(id);

    if (stampByName == null)
      return null;

    return stampByName.getOrDefault(name.toLowerCase(), null);
  }

  private void loadFromDisk() {
    if (!dataFile.isFile())
      return;

    try (
      var reader = new FileReader(dataFile, Charsets.UTF_8);
    ) {
      var jsonData = GSON_INSTANCE.fromJson(reader, JsonObject.class);

      if (jsonData == null)
        return;

      for (var jsonEntry : jsonData.entrySet()) {
        var idString = jsonEntry.getKey();

        UUID id;

        try {
          id = UUID.fromString(idString);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Could not parse ID \"" + idString + "\" of state-file at " + dataFile, e);
          continue;
        }

        if (!(jsonEntry.getValue() instanceof JsonObject stampByNameObject)) {
          logger.warning("Value at ID \"" + idString + "\" of state-file at " + dataFile + " was not a map");
          continue;
        }

        var stampByName = new HashMap<String, String>();

        for (var stampEntry : stampByNameObject.entrySet()) {
          var name = stampEntry.getKey();

          if (!(stampEntry.getValue() instanceof JsonPrimitive valuePrimitive)) {
            logger.warning("Value at ID \"" + idString + "\" and name \"" + name + "\" of state-file at " + dataFile + " was not a primitive");
            continue;
          }

          stampByName.put(name, valuePrimitive.getAsString());
        }

        namedStampsByUid.put(id, stampByName);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not read state-file at " + dataFile, e);
    }
  }

  public void saveToDisk() {
    if (!isDataDirty)
      return;

    isDataDirty = false;

    try (
      var fileWriter = new FileWriter(dataFile, Charsets.UTF_8);
      var jsonWriter = new JsonWriter(fileWriter);
    ) {
      GSON_INSTANCE.toJson(this.namedStampsByUid, Map.class, jsonWriter);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not write state-file to " + dataFile, e);
    }
  }
}
