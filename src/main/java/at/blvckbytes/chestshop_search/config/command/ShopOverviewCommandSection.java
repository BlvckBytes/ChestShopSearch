package at.blvckbytes.chestshop_search.config.command;

import me.blvckbytes.bukkitevaluable.section.ACommandSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class ShopOverviewCommandSection extends ACommandSection {

  public static final String INITIAL_NAME = "shopoverview";

  public ShopOverviewCommandSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(INITIAL_NAME, baseEnvironment);
  }
}
