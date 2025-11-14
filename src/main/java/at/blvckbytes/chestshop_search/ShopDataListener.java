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
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShopDataListener implements Listener {

  private final Plugin plugin;
  private final ChestShopRegistry chestShopRegistry;
  private final ConfigKeeper<MainSection> config;
  private final Logger logger;
  public final Set<ProtectedRegion> shopRegions;

  public ShopDataListener(
    Plugin plugin,
    ChestShopRegistry chestShopRegistry,
    Set<ProtectedRegion> shopRegions,
    ConfigKeeper<MainSection> config,
    Logger logger
  ) {
    this.plugin = plugin;
    this.chestShopRegistry = chestShopRegistry;
    this.shopRegions = shopRegions;
    this.config = config;
    this.logger = logger;
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
    Bukkit.getScheduler().runTask(plugin, () -> possiblyRegisterShop(event.getSign(), event.getSignLines(), false));
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

        //noinspection deprecation
        possiblyRegisterShop(sign, sign.getLines(), true);
      }
    }
  }

  private void possiblyRegisterShop(Sign shopSign, String[] signLines, boolean wasOnChunkLoad) {
    var signLocation = shopSign.getLocation();

    if (wasOnChunkLoad && chestShopRegistry.getShopAtLocation(signLocation) != null)
      return;

    var itemParseEvent = new ItemParseEvent(ChestShopSign.getItem(signLines));
    Bukkit.getPluginManager().callEvent(itemParseEvent);
    var shopItem = itemParseEvent.getItem();

    if (shopItem == null || shopItem.getType() == Material.AIR) {
      logger.log(Level.WARNING, "Item-response was null/AIR for shop at " + signLocation);
      return;
    }

    var owner = ChestShopSign.getOwner(signLines);

    if (owner.isBlank()) {
      logger.log(Level.WARNING, "Owner was blank for shop at " + signLocation);
      return;
    }

    var priceLine = ChestShopSign.getPrice(signLines);
    var buyPrice = PriceUtil.getExactBuyPrice(priceLine).doubleValue();
    var sellPrice = PriceUtil.getExactSellPrice(priceLine).doubleValue();

    if (buyPrice <= 0 && sellPrice <= 0) {
      logger.log(Level.WARNING, "No prices for shop at " + signLocation);
      return;
    }

    int stock = -1;
    int size = -1;

    // Manually look up the container, as ChestShop's utility disregards unloaded blocks, and
    // the container could be on an exact chunk-boundary; this will load said chunk if necessary.
    if (shopSign.getBlockData() instanceof WallSign wallSign) {
      var mountedOnFace = wallSign.getFacing().getOppositeFace();

      var mountedOnBlock = shopSign.getLocation()
        .add(mountedOnFace.getModX(), mountedOnFace.getModY(), mountedOnFace.getModZ())
        .getBlock();

      if (mountedOnBlock.getState() instanceof Container container) {
        stock = InventoryUtil.getAmount(shopItem, container.getInventory());
        size = container.getInventory().getSize();
      }
    }

    var quantity = ChestShopSign.getQuantity(signLines);

    if (quantity <= 0) {
      logger.log(Level.WARNING, "No quantity for shop at " + signLocation);
      return;
    }

    var shopEntry = new ChestShopEntry(
      shopItem,
      owner,
      signLocation,
      quantity,
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
