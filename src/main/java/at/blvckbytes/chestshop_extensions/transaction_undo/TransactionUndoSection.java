package at.blvckbytes.chestshop_extensions.transaction_undo;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class TransactionUndoSection extends ConfigSection {

  public int undoWindowSeconds;
  public ComponentMarkup undoMessage;

  public TransactionUndoSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (undoWindowSeconds < 0)
      throw new IllegalStateException("\"undoWindowSeconds\" cannot be less than zero");
  }
}
