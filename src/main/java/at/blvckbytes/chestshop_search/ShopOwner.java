package at.blvckbytes.chestshop_search;

import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.jetbrains.annotations.Nullable;

public class ShopOwner {

  public final String name;
  public final IEvaluationEnvironment environment;

  private @Nullable String textures;

  public ShopOwner(String name, SkullTexturesManager texturesManager) {
    this.name = name;
    texturesManager.getBase64TexturesIfExist(name, textures -> this.textures = textures);

    this.environment = new EvaluationEnvironmentBuilder()
      .withStaticVariable("owner", name)
      .withLiveVariable("textures", () -> this.textures)
      .build();
  }
}
