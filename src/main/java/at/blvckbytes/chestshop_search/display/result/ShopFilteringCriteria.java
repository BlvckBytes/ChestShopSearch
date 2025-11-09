package at.blvckbytes.chestshop_search.display.result;

import at.blvckbytes.chestshop_search.ChestShopEntry;

import java.util.Arrays;
import java.util.List;

public enum ShopFilteringCriteria implements FilteringFunction {

  CAN_BUY((shop, negative) -> (shop.buyPrice >= 0) ^ negative),
  CAN_SELL((shop, negative) -> (shop.sellPrice >= 0) ^ negative),
  HAS_STOCK_LEFT((shop, negative) -> {
    if (shop.buyPrice < 0)
      return false;

    return (shop.stock > 0) ^ negative;
  }),
  HAS_SPACE_LEFT((shop, negative) -> {
    if (shop.sellPrice < 0)
      return false;

    return (shop.calculateSpace() > 0) ^ negative;
  }),
  ;

  private final FilteringFunction predicate;

  public static final List<ShopFilteringCriteria> values = Arrays.stream(values()).toList();

  ShopFilteringCriteria(FilteringFunction predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(ChestShopEntry shop, boolean negative) {
    return predicate.test(shop, negative);
  }

  public ShopFilteringCriteria next() {
    return values.get((ordinal() + 1) % values.size());
  }

  public static ShopFilteringCriteria byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.size())
      return values.get(0);

    return values.get(ordinal);
  }
}
