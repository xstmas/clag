package clag;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.lang.reflect.Field;

// based on https://github.com/nallar/TickProfiler/blob/master/src/common/me/nallar/tickprofiler/minecraft/entitylist/LoadedTileEntityList.java
public class LoadedTileEntityListThermos extends EntityListThermos<TileEntity> {
	public LoadedTileEntityListThermos(World world, Field overriddenField) {
		super(world, overriddenField);
	}

	@Override
	public void tick() {
		// see https://github.com/nallar/TickProfiler/blob/master/src/common/me/nallar/tickprofiler/minecraft/profiling/EntityTickProfiler.java
		//EntityTickProfiler.ENTITY_TICK_PROFILER.runTileEntities(world, innerList);
		CLagTileEntityTicker.instance.runTileEntities(world, innerList);
	}
}
