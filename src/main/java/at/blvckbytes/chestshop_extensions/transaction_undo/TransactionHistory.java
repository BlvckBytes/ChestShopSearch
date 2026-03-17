package at.blvckbytes.chestshop_extensions.transaction_undo;

import at.blvckbytes.chestshop_extensions.TransactionItem;
import at.blvckbytes.chestshop_extensions.config.MainSection;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;

public class TransactionHistory {

  private final ConfigKeeper<MainSection> config;
  private final Map<UUID, List<UndoableTransaction>> recentTransactionsByWorldId;

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
      .add(new UndoableTransaction(TransactionBlock.fromSign(shopSign), transactionType, transactionItem, exactPrice, System.currentTimeMillis()));
  }

  public @Nullable UndoableTransaction findLastCorrespondingTransaction(
    Sign shopSign,
    TransactionEvent.TransactionType transactionType,
    TransactionItem transactionItem
  ) {
    var bucket = recentTransactionsByWorldId.get(shopSign.getWorld().getUID());

    if (bucket == null || bucket.isEmpty())
      return null;

    var transactionBlock = TransactionBlock.fromSign(shopSign);

    bucket.removeIf(undoableTransaction -> {
      if (undoableTransaction.hasBeenUsedForUndoing())
        return true;

      var ageSeconds = (System.currentTimeMillis() - undoableTransaction.timestamp) / 1000;

      return ageSeconds > config.rootSection.transactionUndo.undoWindowSeconds;
    });

    for (int index = bucket.size() - 1; index >= 0; --index) {
      var transaction = bucket.get(index);

      if (!transactionBlock.equals(transaction.transactionBlock))
        continue;

      // We're seeking to undo a transaction, so the current type has to be the opposite of what it was prior
      if (transactionType == transaction.transactionType)
        continue;

      if (!transactionItem.itemClone.isSimilar(transaction.transactionItem.itemClone))
        continue;

      return transaction;
    }

    return null;
  }
}

