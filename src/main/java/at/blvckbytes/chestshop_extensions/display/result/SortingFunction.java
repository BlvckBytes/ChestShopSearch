package at.blvckbytes.chestshop_extensions.display.result;

import at.blvckbytes.chestshop_extensions.ChestShopEntry;

@FunctionalInterface
public interface SortingFunction {

  int compare(ChestShopEntry a, ChestShopEntry b, boolean descending);

}
