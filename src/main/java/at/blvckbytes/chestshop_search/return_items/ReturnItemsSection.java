package at.blvckbytes.chestshop_search.return_items;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class ReturnItemsSection extends ConfigSection {

  public int returnWindowSeconds;
  public ComponentMarkup returnMessage;

  public ReturnItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (returnWindowSeconds < 0)
      throw new IllegalStateException("\"returnWindowSeconds\" cannot be less than zero");
  }
}
