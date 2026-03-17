package at.blvckbytes.chestshop_search.return_items;

import at.blvckbytes.chestshop_search.TransactionItem;
import com.Acrobot.ChestShop.Events.TransactionEvent;

import java.math.BigDecimal;

public class ReturnableTransaction {

  public final TransactionBlock transactionBlock;
  public final TransactionEvent.TransactionType transactionType;
  public final TransactionItem transactionItem;
  public final BigDecimal exactPrice;
  public final long timestamp;

  private boolean usedForReturning;

  public ReturnableTransaction(TransactionBlock transactionBlock, TransactionEvent.TransactionType transactionType, TransactionItem transactionItem, BigDecimal exactPrice, long timestamp) {
    this.transactionBlock = transactionBlock;
    this.transactionType = transactionType;
    this.transactionItem = transactionItem;
    this.exactPrice = exactPrice;
    this.timestamp = timestamp;
  }

  public void markUsedForReturning() {
    if (usedForReturning)
      throw new IllegalStateException("Already marked as used");

    this.usedForReturning = true;
  }

  public boolean hasBeenUsedForReturning() {
    return usedForReturning;
  }
}