package at.blvckbytes.chestshop_extensions.display.result;

import at.blvckbytes.chestshop_extensions.ChestShopEntry;

@FunctionalInterface
public interface FilteringFunction {

  boolean test(ChestShopEntry shop, boolean negative);

}
