package at.blvckbytes.chestshop_search.display.result;

import at.blvckbytes.chestshop_search.ChestShopEntry;

@FunctionalInterface
public interface FilteringFunction {

  boolean test(ChestShopEntry shop, boolean negative);

}
