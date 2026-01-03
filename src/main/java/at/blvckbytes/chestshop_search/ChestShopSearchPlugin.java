package at.blvckbytes.chestshop_search;

import at.blvckbytes.chestshop_search.command.*;
import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.chestshop_search.config.command.*;
import at.blvckbytes.chestshop_search.display.overview.OverviewDisplayHandler;
import at.blvckbytes.chestshop_search.display.result.ResultDisplayHandler;
import at.blvckbytes.chestshop_search.display.result.SelectionStateStore;
import com.cryptomorin.xseries.XMaterial;
import me.blvckbytes.bukkitevaluable.CommandUpdater;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.ConfigManager;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class ChestShopSearchPlugin extends JavaPlugin {

  private @Nullable ChestShopRegistry chestShopRegistry;
  private @Nullable NameScopedKeyValueStore keyValueStore;
  private @Nullable SelectionStateStore selectionStateStore;
  private @Nullable ResultDisplayHandler resultDisplayHandler;
  private @Nullable OverviewDisplayHandler overviewDisplayHandler;

  @Override
  public void onEnable() {
    var logger = getLogger();

    try {
      // First invocation is quite heavy - warm up cache
      XMaterial.matchXMaterial(Material.AIR);

      keyValueStore = new NameScopedKeyValueStore(getFileAndEnsureExistence("user-preferences.json"), logger);

      var configManager = new ConfigManager(this, "config");
      var config = new ConfigKeeper<>(configManager, "config.yml", MainSection.class);

      var texturesManager = new SkullTexturesManager(this, logger);

      selectionStateStore = new SelectionStateStore(this, logger);

      chestShopRegistry = new ChestShopRegistry(texturesManager, keyValueStore, getFileAndEnsureExistence("known-shops.json"), config, logger);
      Bukkit.getScheduler().runTaskAsynchronously(this, chestShopRegistry::load);

      resultDisplayHandler = new ResultDisplayHandler(config, selectionStateStore, chestShopRegistry, logger, this);
      Bukkit.getServer().getPluginManager().registerEvents(resultDisplayHandler, this);

      var dataListener = new ShopDataListener(this, chestShopRegistry, config, logger);
      getServer().getPluginManager().registerEvents(dataListener, this);

      Bukkit.getScheduler().runTaskTimerAsynchronously(this, chestShopRegistry::save, 20L * 30, 20L * 300);

      var parserPlugin = ItemPredicateParserPlugin.getInstance();

      if (parserPlugin == null)
        throw new IllegalStateException("Depending on ItemPredicateParser to be successfully loaded");

      var predicateHelper = parserPlugin.getPredicateHelper();

      Bukkit.getScheduler().runTaskTimerAsynchronously(this, keyValueStore::saveToDisk, 20 * 60L, 20 * 60L);

      overviewDisplayHandler = new OverviewDisplayHandler(resultDisplayHandler, chestShopRegistry, config, this);
      Bukkit.getServer().getPluginManager().registerEvents(overviewDisplayHandler, this);

      var commandUpdater = new CommandUpdater(this);

      var shopSearchCommand = Objects.requireNonNull(getCommand(ShopSearchCommandSection.INITIAL_NAME));
      var shopSearchToggleCommand = Objects.requireNonNull(getCommand(ShopSearchToggleCommandSection.INITIAL_NAME));
      var shopOverviewCommand = Objects.requireNonNull(getCommand(ShopOverviewCommandSection.INITIAL_NAME));
      var shopSearchReloadCommand = Objects.requireNonNull(getCommand(ShopSearchReloadCommandSection.INITIAL_NAME));

      shopSearchCommand.setExecutor(new ShopSearchCommand(chestShopRegistry, predicateHelper, resultDisplayHandler, config));
      shopSearchToggleCommand.setExecutor(new ShopSearchToggleCommand(keyValueStore, dataListener, config));
      shopOverviewCommand.setExecutor(new ShopOverviewCommand(chestShopRegistry, overviewDisplayHandler));
      shopSearchReloadCommand.setExecutor(new ShopSearchReloadCommand(config, logger));

      Runnable updateCommands = () -> {
        config.rootSection.commands.shopSearch.apply(shopSearchCommand, commandUpdater);
        config.rootSection.commands.shopSearchToggle.apply(shopSearchToggleCommand, commandUpdater);
        config.rootSection.commands.shopOverview.apply(shopOverviewCommand, commandUpdater);
        config.rootSection.commands.shopSearchReload.apply(shopSearchReloadCommand, commandUpdater);

        commandUpdater.trySyncCommands();
      };

      updateCommands.run();
      config.registerReloadListener(updateCommands);
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

    if (overviewDisplayHandler != null) {
      overviewDisplayHandler.onShutdown();
      overviewDisplayHandler = null;
    }
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
