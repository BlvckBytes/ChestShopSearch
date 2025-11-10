package at.blvckbytes.chestshop_search.display.overview;

import at.blvckbytes.chestshop_search.ChestShopEntry;
import at.blvckbytes.chestshop_search.ChestShopRegistry;
import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.chestshop_search.display.DisplayHandler;
import at.blvckbytes.chestshop_search.display.result.ResultDisplayData;
import at.blvckbytes.chestshop_search.display.result.ResultDisplayHandler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;

public class OverviewDisplayHandler extends DisplayHandler<OverviewDisplay, OverviewDisplayData> {

  private final ResultDisplayHandler resultDisplayHandler;
  private final ChestShopRegistry shopRegistry;

  public OverviewDisplayHandler(
    ResultDisplayHandler resultDisplayHandler,
    ChestShopRegistry shopRegistry,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin);

    this.resultDisplayHandler = resultDisplayHandler;
    this.shopRegistry = shopRegistry;
  }

  @Override
  public OverviewDisplay instantiateDisplay(Player player, OverviewDisplayData displayData) {
    return new OverviewDisplay(this, config, plugin, player, displayData);
  }

  @Override
  protected void handleClick(Player player, OverviewDisplay display, ClickType clickType, int slot) {
    var targetOwner = display.getOwnerCorrespondingToSlot(slot);

    if (clickType == ClickType.LEFT) {
      if (config.rootSection.resultDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.previousPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.nextPage.getDisplaySlots().contains(slot)) {
        display.nextPage();
        return;
      }

      if (targetOwner != null) {
        var items = new ArrayList<ChestShopEntry>();

        shopRegistry.forEachKnownShop(shop -> {
          if (shop.owner.equalsIgnoreCase(targetOwner.name))
            items.add(shop);
        });

        resultDisplayHandler.show(player, new ResultDisplayData(new OverviewDisplayInfo(display, targetOwner), items));
        return;
      }

      return;
    }

    if (clickType == ClickType.RIGHT) {
      if (config.rootSection.resultDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.firstPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.nextPage.getDisplaySlots().contains(slot))
        display.lastPage();
    }
  }
}
