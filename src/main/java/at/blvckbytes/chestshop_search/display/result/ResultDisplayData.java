package at.blvckbytes.chestshop_search.display.result;

import at.blvckbytes.chestshop_search.ChestShopEntry;
import at.blvckbytes.chestshop_search.display.overview.OverviewDisplayInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public record ResultDisplayData(
  @Nullable OverviewDisplayInfo overviewDisplayInfo,
  Collection<ChestShopEntry> shops
) {}
