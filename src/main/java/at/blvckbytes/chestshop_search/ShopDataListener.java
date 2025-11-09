package at.blvckbytes.chestshop_search;

import at.blvckbytes.chestshop_search.config.MainSection;
import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.Events.ItemParseEvent;
import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import com.Acrobot.ChestShop.Events.ShopDestroyedEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public class ShopDataListener implements Listener {

  private final Plugin plugin;
  private final ChestShopRegistry chestShopRegistry;
  private final ConfigKeeper<MainSection> config;
  public final Set<ProtectedRegion> shopRegions;

  public ShopDataListener(
    Plugin plugin,
    ChestShopRegistry chestShopRegistry,
    Set<ProtectedRegion> shopRegions,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.chestShopRegistry = chestShopRegistry;
    this.shopRegions = shopRegions;
    this.config = config;
  }

  @EventHandler
  public void onShopTransaction(TransactionEvent event) {
    int totalAmountTransferred = 0;

    for (var item : event.getStock())
      totalAmountTransferred += item.getAmount();

    chestShopRegistry.onTransaction(event.getSign().getLocation(), totalAmountTransferred, event.getTransactionType() == TransactionEvent.TransactionType.BUY);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getInventory().getHolder() instanceof Container container))
      return;

    var shopSign = uBlock.getConnectedSign(container);

    if (shopSign == null)
      return;

    var itemParseEvent = new ItemParseEvent(ChestShopSign.getItem(shopSign));
    Bukkit.getPluginManager().callEvent(itemParseEvent);
    var shopItem = itemParseEvent.getItem();

    if (shopItem == null)
      return;

    var stock = InventoryUtil.getAmount(shopItem, event.getInventory());

    chestShopRegistry.onInventoryClose(shopSign.getLocation(), stock, event.getInventory().getSize());
  }

  @EventHandler
  public void onShopCreated(ShopCreatedEvent event) {
    Bukkit.getScheduler().runTask(plugin, () -> possiblyRegisterShop(event.getSign(), false));
  }

  @EventHandler
  public void onShopDestroyed(ShopDestroyedEvent event) {
    chestShopRegistry.onDestruction(event.getSign().getLocation());
  }

  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    var chunk = event.getChunk();

    if (!isChunkInAnyShopRegion(chunk))
      return;

    for (var tileEntity : chunk.getTileEntities()) {
      if (tileEntity instanceof Sign sign) {
        if (!ChestShopSign.isValid(sign))
          continue;

        possiblyRegisterShop(sign, true);
      }
    }
  }

  private void possiblyRegisterShop(Sign shopSign, boolean wasOnChunkLoad) {
    var signLocation = shopSign.getLocation();

    if (wasOnChunkLoad && chestShopRegistry.getShopAtLocation(signLocation) != null)
      return;

    var itemParseEvent = new ItemParseEvent(ChestShopSign.getItem(shopSign));
    Bukkit.getPluginManager().callEvent(itemParseEvent);
    var shopItem = itemParseEvent.getItem();

    if (shopItem == null)
      return;

    var owner = ChestShopSign.getOwner(shopSign);

    if (owner.isBlank())
      return;

    var priceLine = ChestShopSign.getPrice(shopSign);
    var buyPrice = PriceUtil.getExactBuyPrice(priceLine).doubleValue();
    var sellPrice = PriceUtil.getExactSellPrice(priceLine).doubleValue();

    if (buyPrice < 0 && sellPrice < 0)
      return;

    int stock = -1;
    int size = -1;

    var container = uBlock.findConnectedContainer(shopSign);

    if (container != null) {
      stock = InventoryUtil.getAmount(shopItem, container.getInventory());
      size = container.getInventory().getSize();
    }

    var shopEntry = new ChestShopEntry(
      shopItem,
      owner,
      signLocation,
      ChestShopSign.getQuantity(shopSign),
      buyPrice,
      sellPrice,
      stock,
      size,
      config
    );

    chestShopRegistry.onCreation(shopEntry);
  }

  private boolean isChunkInAnyShopRegion(Chunk chunk) {
    var chunkX = chunk.getX();
    var chunkZ = chunk.getZ();
    var minBlockX = chunkX << 4;
    var minBlockZ = chunkZ << 4;
    var maxBlockX = minBlockX + 15;
    var maxBlockZ = minBlockZ + 15;

    for (var region : shopRegions) {
      BlockVector3 regionMin = region.getMinimumPoint();
      BlockVector3 regionMax = region.getMaximumPoint();

      if (regionMax.x() < minBlockX || regionMin.x() > maxBlockX)
        continue;

      if (regionMax.z() < minBlockZ || regionMin.z() > maxBlockZ)
        continue;

      return true;
    }

    return false;
  }
}
