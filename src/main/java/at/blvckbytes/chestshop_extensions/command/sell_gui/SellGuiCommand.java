package at.blvckbytes.chestshop_extensions.command.sell_gui;

import at.blvckbytes.chestshop_extensions.ChestShopRegistry;
import at.blvckbytes.chestshop_extensions.MutableInt;
import at.blvckbytes.chestshop_extensions.command.BuySellCommands;
import at.blvckbytes.chestshop_extensions.config.MainSection;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class SellGuiCommand implements CommandExecutor, TabCompleter, Listener {

  private final BuySellCommands buySellCommands;
  private final ChestShopRegistry chestShopRegistry;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, Inventory> sellInventoryByPlayerId;

  public SellGuiCommand(
    ChestShopRegistry chestShopRegistry,
    BuySellCommands buySellCommands,
    ConfigKeeper<MainSection> config
  ) {
    this.buySellCommands = buySellCommands;
    this.chestShopRegistry = chestShopRegistry;
    this.config = config;

    this.sellInventoryByPlayerId = new HashMap<>();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    if (!(player.hasPermission("chestshopextensions.sellgui"))) {
      config.rootSection.sellGui.noPermission.sendMessage(player);
      return true;
    }

    var regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    var targetWorld = player.getWorld();

    if (!config.rootSection.sellGui.regionFilter.shopRegionWorlds.contains(targetWorld.getName())) {
      config.rootSection.sellGui.unallowedWorld.sendMessage(player);
      return true;
    }

    var regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));

    if (regionManager == null) {
      config.rootSection.sellGui.unallowedRegion.sendMessage(player);
      return true;
    }

    var isWithinAllowedRegion = regionManager
      .getApplicableRegions(BlockVector3.at(player.getX(), player.getY(), player.getZ()))
      .getRegions().stream()
      .anyMatch(region -> config.rootSection.sellGui.regionFilter.compiledShopRegionPattern.matcher(region.getId()).matches());

    if (!isWithinAllowedRegion) {
      config.rootSection.sellGui.unallowedRegion.sendMessage(player);
      return true;
    }

    var inventoryTitle = config.rootSection.sellGui.inventoryTitle.interpret(
      SlotType.INVENTORY_TITLE,
      new InterpretationEnvironment()
    ).getFirst();

    var sellInventory = Bukkit.createInventory(null, 9 * config.rootSection.sellGui.inventoryRowCount, inventoryTitle);

    sellInventoryByPlayerId.put(player.getUniqueId(), sellInventory);

    player.openInventory(sellInventory);
    config.rootSection.sellGui.openingPrompt.sendMessage(player);
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (event.getPlayer() instanceof Player player)
      handleClosingSellInventory(player);
  }

  private void handleClosingSellInventory(Player player) {
    var sellInventory = sellInventoryByPlayerId.remove(player.getUniqueId());

    if (sellInventory == null)
      return;

    var sellableItems = new ArrayList<ItemAndShop>();
    var unsellableItems = new ArrayList<ItemAndCount>();

    for (var slotIndex = 0; slotIndex < sellInventory.getSize(); ++slotIndex) {
      var currentItem = sellInventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir() || currentItem.getAmount() <= 0)
        continue;

      // Capture the amount before adding the item back into the player's inventory,
      // as it may change down the road when combined with other partial stacks.
      var amount = currentItem.getAmount();

      player.getInventory()
        .addItem(currentItem)
        .values()
        .forEach(item -> player.getWorld().dropItem(player.getEyeLocation(), item));

      var matchingSoldItem = getOrCreateItemEntry(currentItem, sellableItems, () -> {
        var shop = chestShopRegistry.locateValidatedAdminShopToSellItemTo(currentItem);
        return shop == null ? null : new ItemAndShop(currentItem, shop, new MutableInt());
      });

      if (matchingSoldItem != null) {
        matchingSoldItem.count().value += amount;
        continue;
      }

      var matchingUnsoldItem = getOrCreateItemEntry(currentItem, unsellableItems, () -> new ItemAndCount(currentItem, new MutableInt()));

      if (matchingUnsoldItem != null)
        matchingUnsoldItem.count.value += amount;
    }

    if (sellableItems.isEmpty() && unsellableItems.isEmpty()) {
      config.rootSection.sellGui.emptyInventory.sendMessage(player);
      return;
    }

    for (var sellableItem : sellableItems) {
      var signBlock = sellableItem.shop().signLocation.getBlock();
      buySellCommands.simulateParameterizedInteraction(player, signBlock, false, sellableItem.count().value);
    }

    if (!unsellableItems.isEmpty()) {
      config.rootSection.sellGui.unsellableItems.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("items", unsellableItems)
      );
    }
  }

  private <T extends ItemHolder> @Nullable T getOrCreateItemEntry(ItemStack item, List<T> list, Supplier<@Nullable T> creator) {
    return list.stream()
      .filter(it -> it.item().isSimilar(item))
      .findFirst()
      .orElseGet(() -> {
        var newEntry = creator.get();

        if (newEntry == null)
          return null;

        list.add(newEntry);

        return newEntry;
      });
  }
}
