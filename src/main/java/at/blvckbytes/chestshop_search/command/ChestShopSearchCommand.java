package at.blvckbytes.chestshop_search.command;

import at.blvckbytes.chestshop_search.ChestShopEntry;
import at.blvckbytes.chestshop_search.ChestShopRegistry;
import at.blvckbytes.chestshop_search.UidScopedKeyValueStore;
import at.blvckbytes.chestshop_search.config.MainSection;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.TranslationLanguageRegistry;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.StringifyState;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ChestShopSearchCommand implements CommandExecutor, TabCompleter {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

  private final ChestShopRegistry chestShopRegistry;
  private final PredicateHelper predicateHelper;
  private final TranslationLanguageRegistry languageRegistry;
  private final UidScopedKeyValueStore keyValueStore;
  private final ConfigKeeper<MainSection> config;

  public ChestShopSearchCommand(
    ChestShopRegistry chestShopRegistry,
    PredicateHelper predicateHelper,
    TranslationLanguageRegistry languageRegistry,
    UidScopedKeyValueStore keyValueStore,
    ConfigKeeper<MainSection> config
  ) {
    this.chestShopRegistry = chestShopRegistry;
    this.predicateHelper = predicateHelper;
    this.languageRegistry = languageRegistry;
    this.keyValueStore = keyValueStore;
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

    var language = getUserLanguageOrDefault(player);

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

    for (var result : results) {
      var message = Component.text("- ").color(NamedTextColor.GRAY)
        .append(Component.text(result.owner).color(NamedTextColor.GREEN))
        .append(Component.text(" | ").color(NamedTextColor.GRAY))
        .append(Component.text(result.quantity + "x").color(NamedTextColor.GREEN));

      if (result.buyPrice >= 0) {
        message = message
          .append(Component.text(" | ").color(NamedTextColor.GRAY))
          .append(Component.text("B " + DECIMAL_FORMAT.format(result.buyPrice) + " (" + result.stock + " übrig)").color(NamedTextColor.GREEN));
      }

      if (result.sellPrice >= 0) {
        message = message
          .append(Component.text(" | ").color(NamedTextColor.GRAY))
          .append(Component.text("S " + DECIMAL_FORMAT.format(result.sellPrice) + " (" + result.calculateSpace() + " Platz)").color(NamedTextColor.GREEN));
      }

      message = message
        .append(Component.text(" | ").color(NamedTextColor.GRAY))
        .append(
          Component.text(getTranslationOrFallback(language, result.item))
            .hoverEvent(result.item.asHoverEvent())
            .color(NamedTextColor.GREEN)
            .decorate(TextDecoration.UNDERLINED)
        );

      var locationString = result.signLocation.getWorld().getName() + " " + result.signLocation.getBlockX() + " " + result.signLocation.getBlockY() + " " + result.signLocation.getBlockZ();

      player.sendMessage(message.clickEvent(ClickEvent.runCommand("/shopteleport " + locationString)));
    }

    return true;
  }

  private String getTranslationOrFallback(TranslationLanguage language, ItemStack item) {
    var translationRegistry = languageRegistry.getTranslationRegistry(language);
    var translation = translationRegistry.getTranslationBySingleton(item.getType());
    return translation == null ? item.getType().name() : translation;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return null;

    var language = getUserLanguageOrDefault(player);

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

  private TranslationLanguage getUserLanguageOrDefault(Player player) {
    var languageName = keyValueStore.read(player.getUniqueId(), UidScopedKeyValueStore.KEY_QUERY_LANGUAGE);

    if (languageName == null)
      return TranslationLanguage.GERMAN_DE;

    try {
      return TranslationLanguage.valueOf(languageName);
    } catch (Throwable e) {
      return TranslationLanguage.GERMAN_DE;
    }
  }
}
