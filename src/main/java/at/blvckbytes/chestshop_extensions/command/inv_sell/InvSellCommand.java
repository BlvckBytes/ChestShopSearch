package at.blvckbytes.chestshop_extensions.command.inv_sell;

import at.blvckbytes.chestshop_extensions.ChestShopRegistry;
import at.blvckbytes.chestshop_extensions.OfflinePlayerRegistry;
import at.blvckbytes.chestshop_extensions.eco_log.EcoLogger;
import at.blvckbytes.chestshop_extensions.command.sell_gui.SellGuiCommand;
import at.blvckbytes.chestshop_extensions.command.sell_gui.SellToShopSession;
import at.blvckbytes.chestshop_extensions.config.MainSection;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InvSellCommand implements CommandExecutor, TabCompleter, Listener {

  private record FilterViewingSession(Player viewer, OfflinePlayer owner, boolean editable, Inventory inventory) {}

  private final ChestShopRegistry shopRegistry;
  private final Economy economy;
  private final @Nullable EcoLogger ecoLogger;
  private final OfflinePlayerRegistry offlinePlayerRegistry;

  private final Logger logger;
  private final ConfigKeeper<MainSection> config;

  private final List<FilterViewingSession> viewingSessions;

  private final NamespacedKey filtersItemDataKey;

  public InvSellCommand(
    Plugin plugin,
    ChestShopRegistry shopRegistry,
    Economy economy,
    @Nullable EcoLogger ecoLogger,
    OfflinePlayerRegistry offlinePlayerRegistry,
    ConfigKeeper<MainSection> config
  ) {
    this.logger = plugin.getLogger();
    this.shopRegistry = shopRegistry;
    this.economy = economy;
    this.ecoLogger = ecoLogger;
    this.offlinePlayerRegistry = offlinePlayerRegistry;
    this.config = config;

    this.viewingSessions = new ArrayList<>();

    this.filtersItemDataKey = new NamespacedKey(plugin, "inv-sell-filters");
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    if (!(player.hasPermission("chestshopextensions.invsell"))) {
      config.rootSection.sellGui.invSell.noPermission.sendMessage(player);
      return true;
    }

    if (SellGuiCommand.isOutsideOfAllowedRegionAndSendMessages(player, config))
      return true;

    if (args.length == 0) {
      handleSellingInventory(player, label);
      return true;
    }

    var actionConstant = CommandAction.matcher.matchFirst(args[0]);

    if (actionConstant == null) {
      config.rootSection.sellGui.invSell.commandUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );
      return true;
    }

    if (actionConstant.constant == CommandAction.FILTERS) {
      OfflinePlayer target = player;

      if (args.length == 2) {
        if (!player.hasPermission("chestshopextensions.invsell.filters.others")) {
          config.rootSection.sellGui.invSell.filtersOtherMissingPermission.sendMessage(player);
          return true;
        }

        target = offlinePlayerRegistry.getPlayerByName(args[1]);

        if (target == null) {
          config.rootSection.sellGui.invSell.filtersOtherUnknownName.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("name", args[1])
          );
          return true;
        }
      }

      var targetId = target.getUniqueId();
      var existingSession = accessFirstMatching(it -> it.owner.getUniqueId().equals(targetId), false);

      if (existingSession != null) {
        config.rootSection.sellGui.invSell.filtersOtherCurrentlyViewed.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("target_name", target.getName())
            .withVariable("viewer_name", existingSession.viewer.getName())
        );
        return true;
      }

      var filtersInventory = makeFiltersInventory();
      loadFiltersFromPlayerPDCAndGetIfEmpty(target, filtersInventory);

      viewingSessions.add(new FilterViewingSession(player, target, target.isOnline(), filtersInventory));

      player.openInventory(filtersInventory);

      config.rootSection.sellGui.invSell.openingFiltersInventory.sendMessage(player);

      if (!player.getUniqueId().equals(targetId)) {
        config.rootSection.sellGui.invSell.filtersOtherNowViewing.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("target_name", target.getName())
        );
      }

      return true;
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !(player.hasPermission("chestshopextensions.invsell")))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    if (args.length == 2 && player.hasPermission("chestshopextensions.invsell.filters.others")) {
      var typedNameLower = args[1].toLowerCase();

      return offlinePlayerRegistry.streamKnownNames()
        .filter(it -> it.toLowerCase().startsWith(typedNameLower))
        .limit(15)
        .toList();
    }

    return List.of();
  }

  @EventHandler(ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    handleInventoryInteraction(event);
  }

  @EventHandler(ignoreCancelled = true)
  public void onInventoryDrag(InventoryDragEvent event) {
    handleInventoryInteraction(event);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player))
      return;

    var viewingSession = accessFirstMatching(it -> event.getInventory().equals(it.inventory), true);

    if (viewingSession == null)
      return;

    var owningPlayer = viewingSession.owner.getPlayer();

    if (!viewingSession.editable || owningPlayer == null) {
      viewingSession.inventory.clear();
      return;
    }

    handleStoringFilterInventory(owningPlayer, player, viewingSession.inventory);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var player = event.getPlayer();

    var viewingSession = accessFirstMatching(it -> it.owner.getUniqueId().equals(player.getUniqueId()), true);

    if (viewingSession == null)
      return;

    viewingSession.viewer.closeInventory();

    config.rootSection.sellGui.invSell.filtersOtherTargetWentOffline.sendMessage(
      viewingSession.viewer,
      new InterpretationEnvironment()
        .withVariable("target_name", player.getName())
    );

    handleStoringFilterInventory(player, viewingSession.viewer, viewingSession.inventory);
  }

  private void handleStoringFilterInventory(Player owningPlayer, Player viewingPlayer, Inventory inventory) {
    var viewerInventory = viewingPlayer.getInventory();
    var shulkerCount = 0;

    for (var slotIndex = 0; slotIndex < inventory.getSize(); ++slotIndex) {
      var currentItem = inventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getAmount() <= 0 || !Tag.SHULKER_BOXES.isTagged(currentItem.getType()))
        continue;

      shulkerCount += currentItem.getAmount();

      for (var remainder : viewerInventory.addItem(currentItem).values())
        viewingPlayer.getWorld().dropItem(viewingPlayer.getEyeLocation(), remainder);

      inventory.setItem(slotIndex, null);
    }

    if (shulkerCount > 0) {
      config.rootSection.sellGui.invSell.filtersReturnedShulkerBoxes.sendMessage(
        viewingPlayer,
        new InterpretationEnvironment()
          .withVariable("count", shulkerCount)
      );
    }

    storeFiltersToPlayerPDC(owningPlayer, inventory);

    config.rootSection.sellGui.invSell.savedFiltersInventory.sendMessage(
      viewingPlayer,
      new InterpretationEnvironment()
        .withVariable("filter_count", Arrays.stream(inventory.getContents()).filter(this::isValidItem).count())
    );

    inventory.clear();
  }

  private void handleInventoryInteraction(InventoryInteractEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var topInventory = player.getOpenInventory().getTopInventory();
    var viewingSession = accessFirstMatching(it -> topInventory.equals(it.inventory), false);

    if (viewingSession != null && !viewingSession.editable) {
      event.setCancelled(true);

      config.rootSection.sellGui.invSell.filtersOtherReadOnly.sendMessage(
        viewingSession.viewer,
        new InterpretationEnvironment()
          .withVariable("target_name", player.getName())
      );
    }
  }

  private @Nullable FilterViewingSession accessFirstMatching(Predicate<FilterViewingSession> predicate, boolean remove) {
    for (var index = 0; index < viewingSessions.size(); ++index) {
      var viewingSession = viewingSessions.get(index);

      if (predicate.test(viewingSession)) {
        if (remove)
          viewingSessions.remove(index);

        return viewingSession;
      }
    }

    return null;
  }

  private void handleSellingInventory(Player player, String commandLabel) {
    var environment = new InterpretationEnvironment()
      .withVariable("edit_command", "/" + commandLabel + " " + CommandAction.matcher.getNormalizedName(CommandAction.FILTERS));

    var filtersInventory = makeFiltersInventory();

    if (loadFiltersFromPlayerPDCAndGetIfEmpty(player, filtersInventory)) {
      config.rootSection.sellGui.invSell.noFiltersConfigured.sendMessage(player, environment);
      return;
    }

    var filterItems = filtersInventory.getContents();
    var sellSession = new SellToShopSession(shopRegistry, false);

    sellSession.removeItemsToSell(player.getInventory(), itemToSell -> {
      for (var filterItem : filterItems) {
        if (filterItem != null && filterItem.isSimilar(itemToSell))
          return true;
      }

      return false;
    });

    if (!sellSession.sendMessagesAndTransact(player, economy, ecoLogger, config))
      config.rootSection.sellGui.invSell.noMatchingItemsFound.sendMessage(player, environment);
  }

  private Inventory makeFiltersInventory() {
    return Bukkit.createInventory(
      null, 9 * 6,
      config.rootSection.sellGui.invSell.filtersInventoryTitle
        .interpret(SlotType.INVENTORY_TITLE, null)
        .getFirst()
    );
  }

  private void storeFiltersToPlayerPDC(Player player, Inventory filtersInventory) {
    byte[] itemDataBytes;

    try {
      itemDataBytes = ItemStack.serializeItemsAsBytes(filtersInventory.getContents());
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not serialize inv-sell filters of " + player.getName(), e);
      return;
    }

    player.getPersistentDataContainer().set(filtersItemDataKey, PersistentDataType.BYTE_ARRAY, itemDataBytes);
  }

  private boolean loadFiltersFromPlayerPDCAndGetIfEmpty(OfflinePlayer player, Inventory filtersInventory) {
    var itemDataBytes = player.getPersistentDataContainer().get(filtersItemDataKey, PersistentDataType.BYTE_ARRAY);

    if (itemDataBytes == null)
      return false;

    ItemStack[] filterItems;

    try {
      filterItems = ItemStack.deserializeItemsFromBytes(itemDataBytes);
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "Could not deserialize inv-sell filters of " + player.getName(), e);
      return true;
    }

    var filterCount = 0;

    for (var itemIndex = 0; itemIndex < filterItems.length; ++itemIndex) {
      if (itemIndex >= filtersInventory.getSize())
        break;

      var filterItem = filterItems[itemIndex];

      if (!isValidItem(filterItem))
        continue;

      filtersInventory.setItem(itemIndex, filterItem);
      ++filterCount;
    }

    return filterCount == 0;
  }

  private boolean isValidItem(ItemStack item) {
    return item != null && item.getAmount() > 0 && !item.getType().isAir();
  }
}
