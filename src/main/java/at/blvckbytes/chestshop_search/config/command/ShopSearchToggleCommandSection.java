package at.blvckbytes.chestshop_search.config.command;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ShopSearchToggleCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "shopsearchtoggle";

  public ShopSearchToggleCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}
