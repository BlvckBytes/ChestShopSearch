package at.blvckbytes.chestshop_search;

import at.blvckbytes.chestshop_search.config.MainSection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import org.bukkit.Location;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChestShopRegistry {

  private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().create();

  private final NameScopedKeyValueStore keyValueStore;
  private final RegionContainer regionContainer;
  private final File persistenceFile;
  private final ConfigKeeper<MainSection> config;
  private final Logger logger;
  private final Map<WorldAndRegionManager, Long2ObjectMap<ChestShopEntry>> shopByFastHashByWorldId;

  public ChestShopRegistry(
    NameScopedKeyValueStore keyValueStore,
    File persistenceFile,
    ConfigKeeper<MainSection> config,
    Logger logger
  ) {
    this.keyValueStore = keyValueStore;
    this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    this.persistenceFile = persistenceFile;
    this.config = config;
    this.logger = logger;
    this.shopByFastHashByWorldId = new HashMap<>();
  }

  private boolean checkIfShopIsHidden(ChestShopEntry shopEntry, RegionManager regionManager) {
    var regionSet = regionManager.getApplicableRegions(shopEntry.blockVector);
    var ownerName = shopEntry.owner;

    for (var region : regionSet) {
      var visibilityState = keyValueStore.read(ownerName, NameScopedKeyValueStore.makeRegionVisibilityKey(region.getId()));

      if (!"false".equals(visibilityState))
        continue;

      return true;
    }

    return false;
  }

  public void forEachKnownShop(Consumer<ChestShopEntry> consumer) {
    for (var worldBucketEntry : shopByFastHashByWorldId.entrySet()) {
      var regionManager = worldBucketEntry.getKey().regionManager();

      for (var shopEntry : worldBucketEntry.getValue().values()) {
        if (checkIfShopIsHidden(shopEntry, regionManager))
          continue;

        consumer.accept(shopEntry);
      }
    }
  }

  public void save() {
    var jsonShops = new JsonArray();

    for (var worldBucket : shopByFastHashByWorldId.values()) {
      for (var shopEntry : worldBucket.values()) {
        var shopJson = shopEntry.toJson(logger);

        if (shopJson == null)
          continue;

        jsonShops.add(shopJson);
      }
    }

    try (
      var fileWriter = new FileWriter(persistenceFile)
    ) {
      fileWriter.write(GSON_INSTANCE.toJson(jsonShops));
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to save the persistence-file", e);
    }
  }

  public void load() {
    try (
      var fileReader = new FileReader(persistenceFile)
    ) {
      if (!fileReader.ready())
        return;

      this.shopByFastHashByWorldId.clear();

      var jsonShops = GSON_INSTANCE.fromJson(fileReader, JsonArray.class);
      var loadedCounter = 0;

      for (var jsonShop : jsonShops) {
        var shopEntry = ChestShopEntry.fromJson(jsonShop, config, logger);

        if (shopEntry == null)
          continue;

        var signLocation = shopEntry.signLocation;
        var worldBucket = getOrCreateWorldBucket(signLocation);

        if (worldBucket != null)
          worldBucket.put(fastCoordinateHash(signLocation.getBlockX(), signLocation.getBlockY(), signLocation.getBlockZ()), shopEntry);

        ++loadedCounter;
      }

      logger.info("Loaded " + loadedCounter + " shops from the persistence-file.");
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to load the persistence-file", e);
    }
  }

  public @Nullable ChestShopEntry getShopAtLocation(Location signLocation) {
    var worldBucket = getOrCreateWorldBucket(signLocation);

    if (worldBucket != null)
      return worldBucket.get(fastCoordinateHash(signLocation.getBlockX(), signLocation.getBlockY(), signLocation.getBlockZ()));

    return null;
  }

  public void onCreation(ChestShopEntry chestShopEntry) {
    var signLocation = chestShopEntry.signLocation;
    var worldBucket = getOrCreateWorldBucket(signLocation);

    if (worldBucket != null)
      worldBucket.put(fastCoordinateHash(signLocation.getBlockX(), signLocation.getBlockY(), signLocation.getBlockZ()), chestShopEntry);
  }

  public void onDestruction(Location signLocation) {
    var worldBucket = getOrCreateWorldBucket(signLocation);

    if (worldBucket != null)
      worldBucket.remove(fastCoordinateHash(signLocation.getBlockX(), signLocation.getBlockY(), signLocation.getBlockZ()));
  }

  public void onTransaction(Location signLocation, int amount, boolean wasBuy) {
    var worldBucket = getOrCreateWorldBucket(signLocation);

    if (worldBucket != null) {
      var shop = worldBucket.get(fastCoordinateHash(signLocation.getBlockX(), signLocation.getBlockY(), signLocation.getBlockZ()));

      if (shop != null)
        shop.stock += amount * (wasBuy ? -1 : 1);
    }
  }

  public void onInventoryClose(Location signLocation, int stock, int containerSize) {
    var worldBucket = getOrCreateWorldBucket(signLocation);

    if (worldBucket != null) {
      var shop = worldBucket.get(fastCoordinateHash(signLocation.getBlockX(), signLocation.getBlockY(), signLocation.getBlockZ()));

      if (shop != null) {
        shop.stock = stock;
        shop.containerSize = containerSize;
      }
    }
  }

  private @Nullable Long2ObjectMap<ChestShopEntry> getOrCreateWorldBucket(Location signLocation) {
    var signWorld = signLocation.getWorld();

    if (signWorld == null) {
      logger.warning("Encountered null-world location: " + signLocation);
      return null;
    }

    var regionManager = regionContainer.get(BukkitAdapter.adapt(signWorld));

    if (regionManager == null) {
      logger.warning("Could not locate region-manager of world " + signWorld);
      return null;
    }

    return this.shopByFastHashByWorldId.computeIfAbsent(new WorldAndRegionManager(signWorld, regionManager), key -> new Long2ObjectOpenHashMap<>());
  }

  private static long fastCoordinateHash(int x, int y, int z) {
    // y in [-64;320] - adding 64 will result in [0;384], thus 9 bits will suffice
    // long has 64 bits, (64-9)/2 = 27.5, thus, let's reserve 10 bits for y, and add 128, for future-proofing
    // 27 bits per x/z axis, with one sign-bit, => +- 67,108,864
    // As far as I know, the world is limited to around +- 30,000,000 - so we're fine

    return (
      // 2^10 - 1 = 0x3FF
      // 2^26 - 1 = 0x3FFFFFF
      // 2^26     = 0x4000000
      ((y + 128) & 0x3FF) |
      (((x & 0x3FFFFFF) | (x < 0 ? 0x4000000L : 0)) << 10) |
      (((z & 0x3FFFFFF) | (z < 0 ? 0x4000000L : 0)) << (10 + 27))
    );
  }
}
