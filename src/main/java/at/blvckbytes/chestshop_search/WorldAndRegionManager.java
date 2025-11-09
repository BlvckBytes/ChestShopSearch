package at.blvckbytes.chestshop_search;

import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.World;

import java.util.Objects;

public record WorldAndRegionManager(World world, RegionManager regionManager) {

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    WorldAndRegionManager that = (WorldAndRegionManager) o;
    return Objects.equals(world, that.world);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(world);
  }
}
