package at.blvckbytes.chestshop_extensions.command.inv_sell;

import me.blvckbytes.item_predicate_parser.syllables_matcher.EnumMatcher;
import me.blvckbytes.item_predicate_parser.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  FILTERS,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}
