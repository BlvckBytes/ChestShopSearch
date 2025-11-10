package at.blvckbytes.chestshop_search.config.command;

import me.blvckbytes.bukkitevaluable.section.ACommandSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class ShopSearchLanguageCommandSection extends ACommandSection {

  public static final String INITIAL_NAME = "shopsearchlanguage";

  public ShopSearchLanguageCommandSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(INITIAL_NAME, baseEnvironment);
  }
}
