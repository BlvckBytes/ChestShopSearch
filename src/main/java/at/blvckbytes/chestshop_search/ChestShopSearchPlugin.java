package at.blvckbytes.chestshop_search;

import at.blvckbytes.chestshop_search.command.ChestShopSearchCommand;
import at.blvckbytes.chestshop_search.command.ShopSearchLanguageCommand;
import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.chestshop_search.display.result.ResultDisplayHandler;
import at.blvckbytes.chestshop_search.display.result.SelectionStateStore;
import com.cryptomorin.xseries.XMaterial;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.ConfigManager;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class ChestShopSearchPlugin extends JavaPlugin {

  private @Nullable ChestShopRegistry chestShopRegistry;
  private @Nullable UidScopedKeyValueStore keyValueStore;
  private @Nullable SelectionStateStore selectionStateStore;
  private @Nullable ResultDisplayHandler resultDisplayHandler;

  @Override
  public void onEnable() {
    var logger = getLogger();

    try {
      // First invocation is quite heavy - warm up cache
      XMaterial.matchXMaterial(Material.AIR);

      var configManager = new ConfigManager(this, "config");
      var config = new ConfigKeeper<>(configManager, "config.yml", MainSection.class);

      chestShopRegistry = new ChestShopRegistry(getFileAndEnsureExistence("known-shops.json"), config, logger);

      Bukkit.getScheduler().runTaskAsynchronously(this, chestShopRegistry::load);

      // On first tick - when bootup completed
      Bukkit.getScheduler().runTask(this, () -> {
        var dataListener = new ShopDataListener(this, chestShopRegistry, getShopRegions(config), config);
        getServer().getPluginManager().registerEvents(dataListener, this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, chestShopRegistry::save, 20L * 30, 20L * 300);
      });

      var parserPlugin = ItemPredicateParserPlugin.getInstance();

      if (parserPlugin == null)
        throw new IllegalStateException("Depending on ItemPredicateParser to be successfully loaded");

      var predicateHelper = parserPlugin.getPredicateHelper();

      keyValueStore = new UidScopedKeyValueStore(getFileAndEnsureExistence("user-preferences.json"), logger);

      Bukkit.getScheduler().runTaskTimerAsynchronously(this, keyValueStore::saveToDisk, 20L * 5, 20L * 5);

      selectionStateStore = new SelectionStateStore(this, logger);
      resultDisplayHandler = new ResultDisplayHandler(config, selectionStateStore, this);

      Bukkit.getServer().getPluginManager().registerEvents(resultDisplayHandler, this);

      Objects.requireNonNull(getCommand("shopsearch")).setExecutor(new ChestShopSearchCommand(chestShopRegistry, predicateHelper, keyValueStore, resultDisplayHandler, config));
      Objects.requireNonNull(getCommand("shopsearchlanguage")).setExecutor(new ShopSearchLanguageCommand(keyValueStore, config));
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to enable the plugin", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (chestShopRegistry != null) {
      chestShopRegistry.save();
      chestShopRegistry = null;
    }

    if (keyValueStore != null) {
      keyValueStore.saveToDisk();
      keyValueStore = null;
    }

    if (selectionStateStore != null) {
      selectionStateStore.onShutdown();
      selectionStateStore = null;
    }

    if (resultDisplayHandler != null) {
      resultDisplayHandler.onShutdown();
      resultDisplayHandler = null;
    }
  }

  private List<ProtectedRegion> getShopRegions(ConfigKeeper<MainSection> config) {
    var result = new ArrayList<ProtectedRegion>();

    var shopRegionPattern = config.rootSection.regionFilter.compiledShopRegionPattern;
    var shopRegionWorlds = config.rootSection.regionFilter.shopRegionWorlds;
    var regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();

    for (var world : Bukkit.getWorlds()) {
      if (!shopRegionWorlds.contains(world.getName()))
        continue;

      var regionManager = regionContainer.get(BukkitAdapter.adapt(world));

      if (regionManager == null)
        continue;

      for (var regionEntry : regionManager.getRegions().entrySet()) {
        if (!shopRegionPattern.matcher(regionEntry.getKey()).matches())
          continue;

        result.add(regionEntry.getValue());
      }
    }

    if (result.isEmpty())
      getLogger().log(Level.WARNING, "Encountered zero matching shop-regions");
    else
      getLogger().log(Level.INFO, "Encountered " + result.size() + " matching shop-region(s)");

    return result;
  }

  private File getFileAndEnsureExistence(String name) throws Exception {
    var file = new File(getDataFolder(), name);

    if (!file.exists()) {
      var parentDirectory = file.getParentFile();

      if (!parentDirectory.exists() && !parentDirectory.mkdirs())
        throw new IllegalStateException("Could not create parent-directories of the file " + file);

      if (!file.createNewFile())
        throw new IllegalStateException("Could not create the file " + file);
    }

    return file;
  }
}
