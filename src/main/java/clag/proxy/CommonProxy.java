package clag.proxy;

import clag.handlers.CLagTickHandler;
import clag.util.CLagUtils;
import cpw.mods.fml.common.FMLCommonHandler;

public class CommonProxy {
	public CommonProxy() {
		CLagUtils.debug("CLag.ClientProxy constructor");
	}

	// Client stuff
	public void registerRenderers() {
		// Nothing here as the server doesn't render graphics or entities!
	}

	public void registerTickHandler() {
		CLagUtils.debug("CLag: CommonProxy.registerTickHandler 01");
		FMLCommonHandler.instance().bus().register(new CLagTickHandler());
		CLagUtils.debug("CLag: CommonProxy.registerTickHandler 02");
	}

}
