package at.blvckbytes.chestshop_search;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import org.jetbrains.annotations.Nullable;

public class ShopOwner {

  public final String name;

  private @Nullable String textures;

  public ShopOwner(String name, SkullTexturesManager texturesManager) {
    this.name = name;
    texturesManager.getBase64TexturesIfExist(name, textures -> this.textures = textures);
  }

  public InterpretationEnvironment getEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("owner", name)
      .withVariable("textures", this.textures);
  }
}
