package at.blvckbytes.chestshop_search.display.result;

import at.blvckbytes.chestshop_search.ChestShopEntry;

import java.util.Arrays;
import java.util.List;

public enum ShopSortingCriteria implements SortingFunction {

  BUYING_PRICE((a, b, d) -> priceComparator(a.buyPrice, b.buyPrice, d)),
  SELLING_PRICE((a, b, d) -> priceComparator(a.sellPrice, b.sellPrice, d)),
  OWNER_NAME((a, b, d) -> a.owner.compareTo(b.owner) * (d ? -1 : 1)),
  STOCK_LEFT((a, b, d) -> Integer.compare(a.stock, b.stock) * (d ? -1 : 1)),
  SPACE_LEFT((a, b, d) -> Integer.compare(a.calculateSpace(), b.calculateSpace()) * (d ? -1 : 1)),
  ITEM_TYPE((a, b, d) -> a.item.getType().compareTo(b.item.getType()) * (d ? -1 : 1)),
  ;

  private static int priceComparator(double a, double b, boolean descending) {
    if (a < 0 && b < 0)
      return 0;

    // 'a' goes to the end
    if (a < 0)
      return 1;

    // 'b' goes to the end
    if (b < 0)
      return -1;

    // Standard comparison, now acknowledging direction
    return Double.compare(a, b) * (descending ? -1 : 1);
  }

  private final SortingFunction function;

  public static final List<ShopSortingCriteria> values = Arrays.stream(values()).toList();

  ShopSortingCriteria(SortingFunction function) {
    this.function = function;
  }

  @Override
  public int compare(ChestShopEntry a, ChestShopEntry b, boolean descending) {
    return function.compare(a, b, descending);
  }

  public static ShopSortingCriteria byOrdinalOrFirst(int ordinal) {
    if (ordinal < 0 || ordinal >= values.size())
      return values.get(0);

    return values.get(ordinal);
  }
}
