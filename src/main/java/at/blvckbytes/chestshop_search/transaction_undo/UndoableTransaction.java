package at.blvckbytes.chestshop_search.transaction_undo;

import at.blvckbytes.chestshop_search.TransactionItem;
import com.Acrobot.ChestShop.Events.TransactionEvent;

import java.math.BigDecimal;

public class UndoableTransaction {

  public final TransactionBlock transactionBlock;
  public final TransactionEvent.TransactionType transactionType;
  public final TransactionItem transactionItem;
  public final BigDecimal exactPrice;
  public final long timestamp;

  private boolean usedForUndoing;

  public UndoableTransaction(TransactionBlock transactionBlock, TransactionEvent.TransactionType transactionType, TransactionItem transactionItem, BigDecimal exactPrice, long timestamp) {
    this.transactionBlock = transactionBlock;
    this.transactionType = transactionType;
    this.transactionItem = transactionItem;
    this.exactPrice = exactPrice;
    this.timestamp = timestamp;
  }

  public void markUsedForUndoing() {
    if (usedForUndoing)
      throw new IllegalStateException("Already marked as used");

    this.usedForUndoing = true;
  }

  public boolean hasBeenUsedForUndoing() {
    return usedForUndoing;
  }
}