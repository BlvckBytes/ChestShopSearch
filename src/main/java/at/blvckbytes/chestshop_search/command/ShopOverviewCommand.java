package at.blvckbytes.chestshop_search.command;

import at.blvckbytes.chestshop_search.ChestShopRegistry;
import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.chestshop_search.display.overview.OverviewDisplayData;
import at.blvckbytes.chestshop_search.display.overview.OverviewDisplayHandler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopOverviewCommand implements CommandExecutor {

  private final ChestShopRegistry chestShopRegistry;
  private final OverviewDisplayHandler overviewDisplayHandler;

  public ShopOverviewCommand(
    ChestShopRegistry chestShopRegistry,
    OverviewDisplayHandler overviewDisplayHandler
  ) {
    this.chestShopRegistry = chestShopRegistry;
    this.overviewDisplayHandler = overviewDisplayHandler;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    if (!(sender instanceof Player player))
      return false;

    overviewDisplayHandler.show(player, new OverviewDisplayData(chestShopRegistry.getKnownOwners()));
    return true;
  }
}
