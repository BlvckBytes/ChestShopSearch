package at.blvckbytes.chestshop_search;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public record TransactionItem(ItemStack itemClone, int totalAmount) {

  public static @Nullable TransactionItem of(ItemStack[] stock, Logger logger) {
    int totalAmount = 0;
    ItemStack itemClone = null;

    for (var item : stock) {
      if (item == null || item.getType().isAir() || item.getAmount() < 0)
        continue;

      totalAmount += item.getAmount();

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

    return new TransactionItem(itemClone, totalAmount);
  }
}
