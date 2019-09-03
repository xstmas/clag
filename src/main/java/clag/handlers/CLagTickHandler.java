package clag.handlers;


import clag.CLagTileEntityTicker;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class CLagTickHandler{

	@SubscribeEvent()
	public void tickEvent(TickEvent.ServerTickEvent event) {
		switch (event.phase){
			case START:
				tickStart();
				break;
			case END:
				tickEnd();
				break;
		}
	}

	int iTickNum = 0;
	public void tickStart() {
		++iTickNum;
		//FMLLog.info("CLag.tickStart " + iTickNum);
		CLagTileEntityTicker.instance.StartTick(iTickNum);
	}

	public void tickEnd() {
		CLagTileEntityTicker.instance.EndTick(iTickNum);
	}
}
