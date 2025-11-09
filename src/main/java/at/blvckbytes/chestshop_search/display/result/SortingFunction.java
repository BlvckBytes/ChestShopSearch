package at.blvckbytes.chestshop_search.display.result;

import at.blvckbytes.chestshop_search.ChestShopEntry;

@FunctionalInterface
public interface SortingFunction {

  int compare(ChestShopEntry a, ChestShopEntry b, boolean descending);

}
