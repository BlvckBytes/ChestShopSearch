package at.blvckbytes.chestshop_search.config.command;

import me.blvckbytes.bukkitevaluable.section.ACommandSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class ShopSearchCommandSection extends ACommandSection {

  public static final String INITIAL_NAME = "shopsearch";

  public ShopSearchCommandSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(INITIAL_NAME, baseEnvironment);
  }
}
