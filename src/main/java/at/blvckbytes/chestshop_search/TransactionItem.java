package at.blvckbytes.chestshop_search;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionItem {

  public final ItemStack itemClone;
  public final int totalAmount;
  private final int[] stockAmounts;

  private TransactionItem(ItemStack itemClone, int totalAmount, int[] stockAmounts) {
    this.itemClone = itemClone;
    this.totalAmount = totalAmount;
    this.stockAmounts = stockAmounts;
  }

  public static @Nullable TransactionItem of(ItemStack[] stock, Logger logger) {
    int totalAmount = 0;
    ItemStack itemClone = null;

    int[] stockAmounts = new int[stock.length];

    for (var stockIndex = 0; stockIndex < stock.length; ++stockIndex) {
      var item = stock[stockIndex];

      if (item == null || item.getType().isAir() || item.getAmount() < 0)
        continue;

      var itemAmount = item.getAmount();

      stockAmounts[stockIndex] = itemAmount;

      totalAmount += itemAmount;

      if (itemClone == null) {
        itemClone = new ItemStack(item);
        itemClone.setAmount(1);
        continue;
      }

      if (!itemClone.isSimilar(item)) {
        logger.log(Level.SEVERE, "Expected all items within a transaction to be similar to each-other; ignoring this transaction!");
        return null;
      }
    }

    return new TransactionItem(itemClone, totalAmount, stockAmounts);
  }

  public ItemStack[] recreateStock() {
    var stock = new ItemStack[stockAmounts.length];

    for (var amountIndex = 0; amountIndex < stockAmounts.length; ++amountIndex) {
      var stockItem = new ItemStack(itemClone);

      stockItem.setAmount(stockAmounts[amountIndex]);

      stock[amountIndex] = stockItem;
    }

    return stock;
  }
}
