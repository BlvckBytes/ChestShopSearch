package at.blvckbytes.chestshop_search.return_items;

import at.blvckbytes.chestshop_search.TransactionItem;
import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;

public class TransactionHistory {

  private final ConfigKeeper<MainSection> config;
  private final Map<UUID, List<ReturnableTransaction>> recentTransactionsByWorldId;

  public TransactionHistory(ConfigKeeper<MainSection> config) {
    this.config = config;
    this.recentTransactionsByWorldId = new HashMap<>();
  }

  public void addTransaction(
    Sign shopSign,
    TransactionEvent.TransactionType transactionType,
    TransactionItem transactionItem,
    BigDecimal exactPrice
  ) {
    recentTransactionsByWorldId
      .computeIfAbsent(shopSign.getWorld().getUID(), k -> new ArrayList<>())
      .add(new ReturnableTransaction(shopSign.getBlock(), transactionType, transactionItem, exactPrice, System.currentTimeMillis()));
  }

  public @Nullable ReturnableTransaction findLastCorrespondingTransaction(
    Sign shopSign,
    TransactionEvent.TransactionType transactionType,
    TransactionItem transactionItem
  ) {
    var bucket = recentTransactionsByWorldId.get(shopSign.getWorld().getUID());

    if (bucket == null || bucket.isEmpty())
      return null;

    var signBlock = shopSign.getBlock();

    bucket.removeIf(returnableTransaction -> {
      if (returnableTransaction.hasBeenUsedForReturning())
        return true;

      var ageSeconds = (System.currentTimeMillis() - returnableTransaction.timestamp) / 1000;

      return ageSeconds > config.rootSection.returnItems.returnWindowSeconds;
    });

    for (int index = bucket.size() - 1; index >= 0; --index) {
      var transaction = bucket.get(index);

      if (!signBlock.equals(transaction.signBlock))
        continue;

      // We're seeking to undo a transaction, so the current type has to be the opposite of what it was prior
      if (transactionType == transaction.transactionType)
        continue;

      if (transactionItem.totalAmount() != transaction.transactionItem.totalAmount())
        continue;

      if (!transactionItem.itemClone().isSimilar(transaction.transactionItem.itemClone()))
        continue;

      return transaction;
    }

    return null;
  }
}

