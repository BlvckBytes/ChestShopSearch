package at.blvckbytes.chestshop_search.command;

import at.blvckbytes.chestshop_search.NameScopedKeyValueStore;
import at.blvckbytes.chestshop_search.ShopDataListener;
import at.blvckbytes.chestshop_search.config.MainSection;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ShopSearchToggleCommand implements CommandExecutor {

  private final NameScopedKeyValueStore keyValueStore;
  private final ShopDataListener shopDataListener;
  private final RegionContainer regionContainer;
  private final WorldGuardPlugin worldGuardPlugin;
  private final ConfigKeeper<MainSection> config;

  public ShopSearchToggleCommand(
    NameScopedKeyValueStore keyValueStore,
    ShopDataListener shopDataListener,
    ConfigKeeper<MainSection> config
  ) {
    this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    this.keyValueStore = keyValueStore;
    this.shopDataListener = shopDataListener;
    this.worldGuardPlugin = WorldGuardPlugin.inst();
    this.config = config;
  }

  private List<String> getRegionOwnerNames(ProtectedRegion region) {
    var ownerDomain = region.getOwners();
    var result = new HashSet<>(ownerDomain.getPlayers());

    for (var id : ownerDomain.getUniqueIds())
      result.add(Bukkit.getOfflinePlayer(id).getName());

    return new ArrayList<>(result);
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    var region = locateOwnedShopRegion(player);

    // Yes, this is a bit of a hack... But there may be multiple owners, and I just cannot circumvent that.
    // They're simply all forced onto the same value, with the "first" dictating current state.

    List<String> ownerNames;

    if (region == null || (ownerNames = getRegionOwnerNames(region)).isEmpty()) {
      config.rootSection.playerMessages.shopSearchToggleNotInARegion.sendMessage(player, config.rootSection.builtBaseEnvironment);
      return true;
    }

    var name = ownerNames.getFirst();
    var key = NameScopedKeyValueStore.makeRegionVisibilityKey(region.getId());

    var currentValue = !"false".equals(keyValueStore.read(name, key));
    var nextValue = !currentValue;

    for (var ownerName : ownerNames)
      keyValueStore.write(ownerName, key, String.valueOf(nextValue));

    BukkitEvaluable message;

    if (nextValue)
      message = config.rootSection.playerMessages.shopSearchToggleNowVisible;
    else
      message = config.rootSection.playerMessages.shopSearchToggleNowInvisible;

    message.sendMessage(
      player,
      config.rootSection.getBaseEnvironment()
        .withStaticVariable("owner", name)
        .withStaticVariable("region", region.getId())
        .build()
    );

    return true;
  }

  private @Nullable ProtectedRegion locateOwnedShopRegion(Player player) {
    var regionManager = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));

    if (regionManager == null)
      return null;

    var regions = regionManager.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()).toVector().toBlockPoint());
    var worldGuardPlayer = worldGuardPlugin.wrapPlayer(player);

    for (var region : regions) {
      if (!shopDataListener.isShopRegion(region))
        continue;

      if (!region.isOwner(worldGuardPlayer) && !player.hasPermission("chestshopsearch.toggle.bypass"))
        continue;

      return region;
    }

    return null;
  }
}
