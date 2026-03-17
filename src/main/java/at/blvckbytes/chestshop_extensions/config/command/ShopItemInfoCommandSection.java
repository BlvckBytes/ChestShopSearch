package at.blvckbytes.chestshop_extensions.config.command;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ShopItemInfoCommandSection extends CommandSection {

  public static String INITIAL_NAME = "shopiteminfo";

  public ShopItemInfoCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}
