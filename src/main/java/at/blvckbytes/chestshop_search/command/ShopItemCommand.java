package at.blvckbytes.chestshop_search.command;

import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.Acrobot.ChestShop.Events.ItemParseEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopItemCommand implements CommandExecutor, TabCompleter, Listener {

  private final ConfigKeeper<MainSection> config;
  private final Map<UUID, Inventory> previewInventoryByPlayerId;

  public ShopItemCommand(ConfigKeeper<MainSection> config) {
    this.config = config;
    this.previewInventoryByPlayerId = new HashMap<>();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    //noinspection UnstableApiUsage
    var rayTraceResult = player.getWorld().rayTraceBlocks(
      player.getEyeLocation(),
      player.getEyeLocation().getDirection(),
      4.0,
      FluidCollisionMode.NEVER,
      false,
      block -> Tag.ALL_SIGNS.isTagged(block.getType())
    );

    if (rayTraceResult == null || rayTraceResult.getHitBlock() == null) {
      config.rootSection.playerMessages.notLookingAtShopSign.sendMessage(player);
      return true;
    }

    if (!(rayTraceResult.getHitBlock().getState() instanceof Sign sign)) {
      config.rootSection.playerMessages.notLookingAtShopSign.sendMessage(player);
      return true;
    }

    var itemString = ChestShopSign.getItem(sign);

    if (itemString == null || itemString.isBlank()) {
      config.rootSection.playerMessages.notLookingAtShopSign.sendMessage(player);
      return true;
    }

    var parseEvent = new ItemParseEvent(itemString);

    Bukkit.getServer().getPluginManager().callEvent(parseEvent);

    var item = parseEvent.getItem();

    if (item == null || item.getType().isAir()) {
      config.rootSection.playerMessages.notLookingAtShopSign.sendMessage(player);
      return true;
    }

    var inventory = Bukkit.createInventory(null, 9 * 3, Component.text("/" + label));

    inventory.setItem(13, item);

    player.closeInventory();

    previewInventoryByPlayerId.put(player.getUniqueId(), inventory);

    player.openInventory(inventory);

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
    return List.of();
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    handleInventoryInteraction(event);
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    handleInventoryInteraction(event);
  }

  private void handleInventoryInteraction(InventoryInteractEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var topInventory = player.getOpenInventory().getTopInventory();
    var previewInventory = previewInventoryByPlayerId.get(player.getUniqueId());

    if (previewInventory != null && previewInventory.equals(topInventory))
      event.setCancelled(true);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    var playerId = event.getPlayer().getUniqueId();
    var previewInventory = previewInventoryByPlayerId.get(playerId);

    if (previewInventory != null && previewInventory.equals(event.getInventory()))
      previewInventoryByPlayerId.remove(playerId);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    previewInventoryByPlayerId.remove(event.getPlayer().getUniqueId());
  }
}
