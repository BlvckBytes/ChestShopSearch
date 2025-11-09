package at.blvckbytes.chestshop_search.config;

import me.blvckbytes.bbconfigmapper.MappingError;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegionFilterSection extends AConfigSection {

  public String shopRegionPattern;
  public Pattern compiledShopRegionPattern;
  public List<String> shopRegionWorlds;

  public RegionFilterSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);

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
