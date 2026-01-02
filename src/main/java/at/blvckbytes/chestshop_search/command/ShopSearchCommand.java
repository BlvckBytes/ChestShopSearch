package at.blvckbytes.chestshop_search.command;

import at.blvckbytes.chestshop_search.ChestShopEntry;
import at.blvckbytes.chestshop_search.ChestShopRegistry;
import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.chestshop_search.display.result.ResultDisplayData;
import at.blvckbytes.chestshop_search.display.result.ResultDisplayHandler;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.StringifyState;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ShopSearchCommand implements CommandExecutor, TabCompleter {

  private final ChestShopRegistry chestShopRegistry;
  private final PredicateHelper predicateHelper;
  private final ResultDisplayHandler resultDisplayHandler;
  private final ConfigKeeper<MainSection> config;

  public ShopSearchCommand(
    ChestShopRegistry chestShopRegistry,
    PredicateHelper predicateHelper,
    ResultDisplayHandler resultDisplayHandler,
    ConfigKeeper<MainSection> config
  ) {
    this.chestShopRegistry = chestShopRegistry;
    this.predicateHelper = predicateHelper;
    this.resultDisplayHandler = resultDisplayHandler;
    this.config = config;
  }

  private List<ChestShopEntry> queryShops(@Nullable ItemPredicate predicate) {
    var results = new ArrayList<ChestShopEntry>();

    chestShopRegistry.forEachKnownShop(shop -> {
      if (predicate == null || predicate.test(shop.item))
        results.add(shop);
    });

    return results;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    if (!(sender instanceof Player player))
      return false;

    var language = predicateHelper.getSelectedLanguage(player);

    ItemPredicate predicate = null;

    if (args.length != 0) {

      try {
        var tokens = predicateHelper.parseTokens(args, 0);
        predicate = predicateHelper.parsePredicate(language, tokens);
      } catch (ItemPredicateParseException e) {
        config.rootSection.playerMessages.searchCommandInvalidSearch.sendMessage(
          player,
          config.rootSection.getBaseEnvironment()
            .withStaticVariable("error", predicateHelper.createExceptionMessage(e))
            .build()
        );

        return true;
      }
    }

    var queryString = predicate == null ? "/" : new StringifyState(true).appendPredicate(predicate).toString();
    var results = queryShops(predicate);

    if (results.isEmpty()) {
      config.rootSection.playerMessages.searchCommandNoResults.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("query", queryString)
          .build()
      );

      return true;
    }

    config.rootSection.playerMessages.searchCommandResponse.sendMessage(
      player,
      config.rootSection.getBaseEnvironment()
        .withStaticVariable("query", queryString)
        .withStaticVariable("result_count", results.size())
        .build()
    );

    resultDisplayHandler.show(player, new ResultDisplayData(null, results));

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return null;

    var language = predicateHelper.getSelectedLanguage(player);

    try {
      var tokens = predicateHelper.parseTokens(args, 0);
      var completions = predicateHelper.createCompletion(language, tokens);

      if (completions.expandedPreviewOrError() != null)
        showActionBarMessage(player, completions.expandedPreviewOrError());

      return completions.suggestions();
    } catch (ItemPredicateParseException e) {
      showActionBarMessage(player, predicateHelper.createExceptionMessage(e));
      return null;
    }
  }

  private void showActionBarMessage(Player player, String message) {
    player.sendActionBar(Component.text(message));
  }
}
