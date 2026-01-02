package at.blvckbytes.chestshop_search.display.result;

import at.blvckbytes.chestshop_search.ChestShopEntry;
import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.chestshop_search.display.Display;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResultDisplay extends Display<ResultDisplayData> {

  private final AsyncTaskQueue asyncQueue;

  private List<ChestShopEntry> filteredUnSortedShops;
  private List<ChestShopEntry> filteredSortedShops;

  private final ChestShopEntry[] slotMap;
  private final Set<Location> currentlyRenderedSignLocations;
  private int numberOfPages;
  public final SelectionState selectionState;

  private IEvaluationEnvironment pageEnvironment;
  private IEvaluationEnvironment sortingEnvironment;
  private IEvaluationEnvironment filteringEnvironment;

  private int currentPage = 1;

  private boolean isFirstRender = true;

  public ResultDisplay(
    ConfigKeeper<MainSection> config,
    Plugin plugin,
    Player player,
    ResultDisplayData displayData,
    SelectionState selectionState
  ) {
    super(player, displayData, config, plugin);

    this.asyncQueue = new AsyncTaskQueue(plugin);
    this.slotMap = new ChestShopEntry[9 * 6];
    this.currentlyRenderedSignLocations = new HashSet<>();
    this.selectionState = selectionState;

    setupEnvironments();

    // Within async context already, see corresponding command
    applyFiltering();
    applySorting();
    show();
  }

  private void setupEnvironments() {
    this.pageEnvironment = new EvaluationEnvironmentBuilder()
      .withLiveVariable("current_page", () -> this.currentPage)
      .withLiveVariable("number_pages", () -> this.numberOfPages)
      .withStaticVariable("owner", displayData.overviewDisplayInfo() == null ? null : displayData.overviewDisplayInfo().owner().name)
      .build(config.rootSection.resultDisplay.inventoryEnvironment);

    this.sortingEnvironment = this.selectionState
      .makeSortingEnvironment(config.rootSection)
      .build(pageEnvironment);

    this.filteringEnvironment = this.selectionState
      .makeFilteringEnvironment(config.rootSection)
      .build(pageEnvironment);
  }

  @Override
  public void onConfigReload() {
    setupEnvironments();
    applyFiltering();
    applySorting();
    show();
  }

  public @Nullable ChestShopEntry getShopCorrespondingToSlot(int slot) {
    return slotMap[slot];
  }

  @Override
  public void onInventoryClose() {
    clearSlotMap();
    super.onInventoryClose();
  }

  @Override
  public void onShutdown() {
    clearSlotMap();
    super.onShutdown();
  }

  public void onShopStockChange(ChestShopEntry entry) {
    if (currentlyRenderedSignLocations.contains(entry.signLocation))
      renderItems();
  }

  public void clearSlotMap() {
    for (var i = 0; i < slotMap.length; ++i)
      this.slotMap[i] = null;
  }

  public void nextPage() {
    asyncQueue.enqueue(() -> {
      if (currentPage == numberOfPages)
        return;

      ++currentPage;
      show();
    });
  }

  public void previousPage() {
    asyncQueue.enqueue(() -> {
      if (currentPage == 1)
        return;

      --currentPage;
      show();
    });
  }

  public void firstPage() {
    asyncQueue.enqueue(() -> {
      if (currentPage == 1)
        return;

      currentPage = 1;
      show();
    });
  }

  public void lastPage() {
    asyncQueue.enqueue(() -> {
      if (currentPage == numberOfPages)
        return;

      currentPage = numberOfPages;
      show();
    });
  }

  public void nextSortingSelection() {
    asyncQueue.enqueue(() -> {
      this.selectionState.nextSortingSelection();
      renderSortingItem();
    });
  }

  public void nextSortingOrder() {
    asyncQueue.enqueue(() -> {
      this.selectionState.nextSortingOrder();
      applySorting();
      renderItems();
    });
  }

  public void moveSortingSelectionDown() {
    asyncQueue.enqueue(() -> {
      this.selectionState.moveSortingSelectionDown();
      applySorting();
      renderItems();
    });
  }

  public void resetSortingState() {
    asyncQueue.enqueue(() -> {
      this.selectionState.resetSorting();
      applySorting();
      renderItems();
    });
  }

  public void nextFilteringCriterion() {
    asyncQueue.enqueue(() -> {
      this.selectionState.nextFilteringCriterion();
      renderFilteringItem();
    });
  }

  public void nextFilteringState() {
    asyncQueue.enqueue(() -> {
      this.selectionState.nextFilteringState();
      afterFilterChange();
    });
  }

  public void resetFilteringState() {
    asyncQueue.enqueue(() -> {
      this.selectionState.resetFiltering();
      afterFilterChange();
    });
  }

  private void afterFilterChange() {
    int pageCountDelta = applyFiltering();
    applySorting();

    // Need to update the UI-title
    if (pageCountDelta != 0)
      show();
    else
      renderItems();
  }

  private int applyFiltering() {
    this.filteredUnSortedShops = this.selectionState.applyFilter(displayData.shops());

    var oldNumberOfPages = this.numberOfPages;
    var numberOfDisplaySlots = config.rootSection.resultDisplay.getPaginationSlots().size();

    this.numberOfPages = Math.max(1, (int) Math.ceil(filteredUnSortedShops.size() / (double) numberOfDisplaySlots));

    var pageCountDelta = this.numberOfPages - oldNumberOfPages;

    // Try to stay on the current page, if possible
    if (pageCountDelta < 0)
      this.currentPage = 1;

    return pageCountDelta;
  }

  private void applySorting() {
    this.filteredSortedShops = new ArrayList<>(this.filteredUnSortedShops);
    this.selectionState.applySort(this.filteredSortedShops);
  }

  @Override
  public void show() {
    clearSlotMap();
    super.show();
    isFirstRender = false;
  }

  private void renderSortingItem() {
    config.rootSection.resultDisplay.items.sorting.renderInto(inventory, sortingEnvironment);
  }

  private void renderFilteringItem() {
    config.rootSection.resultDisplay.items.filtering.renderInto(inventory, filteringEnvironment);
  }

  @Override
  protected void renderItems() {
    var displaySlots = config.rootSection.resultDisplay.getPaginationSlots();
    var itemsIndex = (currentPage - 1) * displaySlots.size();
    var numberOfItems = filteredSortedShops.size();

    currentlyRenderedSignLocations.clear();

    for (var slot : displaySlots) {
      var currentSlot = itemsIndex++;

      if (currentSlot >= numberOfItems) {
        slotMap[slot] = null;
        inventory.setItem(slot, null);
        continue;
      }

      var cachedShop = filteredSortedShops.get(currentSlot);

      inventory.setItem(slot, cachedShop.getRepresentativeBuildable().build(
        cachedShop.shopEnvironment
      ));

      slotMap[slot] = cachedShop;
      currentlyRenderedSignLocations.add(cachedShop.signLocation);
    }

    // Render filler first, such that it may be overridden by conditionally displayed items
    config.rootSection.resultDisplay.items.filler.renderInto(inventory, pageEnvironment);

    if (displayData.overviewDisplayInfo() != null)
      config.rootSection.resultDisplay.items.backButton.renderInto(inventory, pageEnvironment);

    config.rootSection.resultDisplay.items.previousPage.renderInto(inventory, pageEnvironment);
    config.rootSection.resultDisplay.items.nextPage.renderInto(inventory, pageEnvironment);
    renderSortingItem();
    renderFilteringItem();

    if (isFirstRender && !displayData.shops().isEmpty() && filteredSortedShops.isEmpty()) {
      config.rootSection.playerMessages.searchCommandBlankUi.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("result_count", displayData.shops().size())
          .build()
      );
    }
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.resultDisplay.createInventory(pageEnvironment);
  }
}
