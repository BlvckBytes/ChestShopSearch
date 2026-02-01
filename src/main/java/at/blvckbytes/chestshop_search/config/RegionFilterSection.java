package at.blvckbytes.chestshop_search.config;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegionFilterSection extends ConfigSection {

  public String shopRegionPattern;
  public Pattern compiledShopRegionPattern;
  public List<String> shopRegionWorlds;

  public RegionFilterSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.shopRegionWorlds = new ArrayList<>();
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    try {
      compiledShopRegionPattern = Pattern.compile(shopRegionPattern);
    } catch (Throwable e) {
      throw new MappingError("Encountered invalid value of shopRegionPattern: " + e.getMessage());
    }
  }
}
