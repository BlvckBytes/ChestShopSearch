package at.blvckbytes.chestshop_search.return_items;

import at.blvckbytes.chestshop_search.TransactionItem;
import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.util.*;

public class ReturnItemsListener implements Listener {

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, TransactionHistory> historyByClientId;

  public ReturnItemsListener(Plugin plugin, ConfigKeeper<MainSection> config) {
    this.plugin = plugin;
    this.config = config;

    this.historyByClientId = new HashMap<>();

    Bukkit.getScheduler().runTaskLater(plugin, this::reorderEventHandlers, 1);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPreTransaction(PreTransactionEvent event) {
    if (!event.getClient().hasPermission("chestshopsearch.return-items") || config.rootSection.returnItems.returnWindowSeconds <= 0)
      return;

    var history = historyByClientId.get(event.getClient().getUniqueId());

    if (history == null)
      return;

    var transactionItem = TransactionItem.of(event.getStock(), plugin.getLogger());

    if (transactionItem == null)
      return;

    var lastCorrespondingTransaction = history.findLastCorrespondingTransaction(event.getSign(), event.getTransactionType(), transactionItem);

    if (lastCorrespondingTransaction == null)
      return;

    event.setExactPrice(lastCorrespondingTransaction.exactPrice);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onTransaction(TransactionEvent event) {
    if (!event.getClient().hasPermission("chestshopsearch.return-items") || config.rootSection.returnItems.returnWindowSeconds <= 0)
      return;

    var transactionItem = TransactionItem.of(event.getStock(), plugin.getLogger());

    if (transactionItem == null)
      return;

    var history = historyByClientId.computeIfAbsent(event.getClient().getUniqueId(), k -> new TransactionHistory(config));

    var lastCorrespondingTransaction = history.findLastCorrespondingTransaction(event.getSign(), event.getTransactionType(), transactionItem);

    if (lastCorrespondingTransaction != null) {
      lastCorrespondingTransaction.markUsedForReturning();
      config.rootSection.returnItems.returnMessage.sendMessage(event.getClient());
      return;
    }

    history.addTransaction(event.getSign(), event.getTransactionType(), transactionItem, event.getExactPrice());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    historyByClientId.remove(event.getPlayer().getUniqueId());
  }

  private void reorderEventHandlers() {
    var handlerList = PreTransactionEvent.getHandlerList();
    var registeredListeners = handlerList.getRegisteredListeners();

    var ourListeners = new ArrayList<RegisteredListener>();

    for (var registeredListener : registeredListeners) {
      handlerList.unregister(registeredListener);

      if (registeredListener.getPlugin() == this.plugin)
        ourListeners.add(registeredListener);
    }

    for (var ourListener : ourListeners)
      handlerList.register(ourListener);

    for (var registeredListener : registeredListeners) {
      if (registeredListener.getPlugin() == this.plugin)
        continue;

      handlerList.register(registeredListener);
    }
  }
}
