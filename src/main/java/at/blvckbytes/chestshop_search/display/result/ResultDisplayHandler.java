package at.blvckbytes.chestshop_search.display.result;

import at.blvckbytes.chestshop_search.ChestShopEntry;
import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.chestshop_search.display.DisplayHandler;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class ResultDisplayHandler extends DisplayHandler<ResultDisplay, ResultDisplayData> {

  private final SelectionStateStore stateStore;

  public ResultDisplayHandler(
    ConfigKeeper<MainSection> config,
    SelectionStateStore stateStore,
    Plugin plugin
  ) {
    super(config, plugin);

    this.stateStore = stateStore;
  }

  public void onStockChange(ChestShopEntry shop) {
    forEachDisplay(display -> display.onShopStockChange(shop));
  }

  @Override
  public ResultDisplay instantiateDisplay(Player player, ResultDisplayData displayData) {
    var selectionState = stateStore.loadState(player);
    return new ResultDisplay(config, plugin, player, displayData, selectionState);
  }

  @Override
  protected void handleClick(Player player, ResultDisplay display, ClickType clickType, int slot) {
    var targetShop = display.getShopCorrespondingToSlot(slot);

    if (clickType == ClickType.LEFT) {
      if (config.rootSection.resultDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.previousPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.backButton.getDisplaySlots().contains(slot)) {
        var overviewInfo = display.displayData.overviewDisplayInfo();

        if (overviewInfo != null)
          overviewInfo.overviewDisplay().reopen();

        return;
      }

      if (config.rootSection.resultDisplay.items.nextPage.getDisplaySlots().contains(slot)) {
        display.nextPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.sorting.getDisplaySlots().contains(slot)) {
        display.nextSortingOrder();
        return;
      }

      if (config.rootSection.resultDisplay.items.filtering.getDisplaySlots().contains(slot)) {
        display.nextFilteringState();
        return;
      }

      if (targetShop != null) {
        player.closeInventory();
        teleportPlayer(player, targetShop);
        return;
      }

      return;
    }

    if (clickType == ClickType.RIGHT) {
      if (config.rootSection.resultDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.firstPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.nextPage.getDisplaySlots().contains(slot)) {
        display.lastPage();
        return;
      }

      if (config.rootSection.resultDisplay.items.sorting.getDisplaySlots().contains(slot)) {
        display.resetSortingState();
        return;
      }

      if (config.rootSection.resultDisplay.items.filtering.getDisplaySlots().contains(slot)) {
        display.resetFilteringState();
        return;
      }

      // Used to open a preview at this point; don't think that's necessary for our server

      return;
    }

    if (clickType == ClickType.DROP) {
      if (config.rootSection.resultDisplay.items.sorting.getDisplaySlots().contains(slot))
        display.nextSortingSelection();

      if (config.rootSection.resultDisplay.items.filtering.getDisplaySlots().contains(slot))
        display.nextFilteringCriterion();

      return;
    }

    if (clickType == ClickType.CONTROL_DROP) {
      if (config.rootSection.resultDisplay.items.sorting.getDisplaySlots().contains(slot))
        display.moveSortingSelectionDown();
    }
  }

  private void teleportPlayer(Player player, ChestShopEntry targetShop) {
    var signBlock = targetShop.signLocation.getBlock();
    var coordinates =  targetShop.signLocation.getBlockX() + " " + targetShop.signLocation.getBlockY() + " " + targetShop.signLocation.getBlockZ();
    var worldName = targetShop.signLocation.getWorld().getName();

    if (!(signBlock.getState() instanceof Sign sign) || !ChestShopSign.isValid(sign)) {
      config.rootSection.playerMessages.shopTeleportShopGone.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable( "coordinates", coordinates)
          .withStaticVariable("world", worldName)
          .withStaticVariable("owner", targetShop.owner)
          .build()
      );

      return;
    }

    var signFacing = ((Directional) signBlock.getBlockData()).getFacing();

    var signCenter = targetShop.signLocation.clone();
    var footLocation = targetShop.signLocation.clone();

    switch (signFacing) {
      case NORTH -> {
        footLocation.add(.5, 0, -.1);
        signCenter.add(.5, .5, .9);
      }

      case SOUTH -> {
        footLocation.add(.5, 0, 1);
        signCenter.add(.5, .5, 0);
      }

      case WEST -> {
        footLocation.add(-.1, 0, .5);
        signCenter.add(.9, .5, .5);
      }

      case EAST -> {
        footLocation.add(1, 0, .5);
        signCenter.add(0, .5, .5);
      }

      default -> { return; }
    }

    var eyeLocation = footLocation.clone().add(0, 1.6, 0);
    var direction = signCenter.toVector().subtract(eyeLocation.toVector()).normalize();
    footLocation.setDirection(direction);

    player.teleport(footLocation);

    config.rootSection.playerMessages.shopTeleportTeleported.sendMessage(
      player,
      config.rootSection.getBaseEnvironment()
        .withStaticVariable("coordinates", coordinates)
        .withStaticVariable("world", worldName)
        .withStaticVariable("owner", targetShop.owner)
        .build()
    );
  }
}
