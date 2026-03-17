package at.blvckbytes.chestshop_extensions.display.result;

import at.blvckbytes.chestshop_extensions.ChestShopEntry;
import at.blvckbytes.chestshop_extensions.display.overview.OverviewDisplayInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public record ResultDisplayData(
  @Nullable OverviewDisplayInfo overviewDisplayInfo,
  Collection<ChestShopEntry> shops
) {}
