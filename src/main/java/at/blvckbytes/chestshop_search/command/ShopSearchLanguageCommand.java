package at.blvckbytes.chestshop_search.command;

import at.blvckbytes.chestshop_search.UidScopedKeyValueStore;
import at.blvckbytes.chestshop_search.config.MainSection;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShopSearchLanguageCommand implements CommandExecutor, TabCompleter {

  private final UidScopedKeyValueStore keyValueStore;
  private final ConfigKeeper<MainSection> config;

  public ShopSearchLanguageCommand(
    UidScopedKeyValueStore keyValueStore,
    ConfigKeeper<MainSection> config
  ) {
    this.keyValueStore = keyValueStore;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    if (args.length != 1) {
      config.rootSection.playerMessages.languageCommandUsage.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("label", label)
          .build()
      );

      return true;
    }

    var languageSelection = TranslationLanguage.matcher.matchFirst(args[0]);

    if (languageSelection == null) {
      config.rootSection.playerMessages.languageCommandUnknownLanguage.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("input", args[0])
          .build()
      );

      return true;
    }

    keyValueStore.write(player.getUniqueId(), UidScopedKeyValueStore.KEY_QUERY_LANGUAGE, languageSelection.constant.name());

    config.rootSection.playerMessages.languageCommandSelectionMade.sendMessage(
      player,
      config.rootSection.getBaseEnvironment()
        .withStaticVariable("language", languageSelection.getNormalizedName())
        .build()
    );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player))
      return List.of();

    if (args.length == 1)
      return TranslationLanguage.matcher.createCompletions(args[0]);

    return List.of();
  }
}
