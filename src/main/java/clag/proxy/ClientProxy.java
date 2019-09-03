package clag.proxy;

import clag.util.CLagUtils;

public class ClientProxy extends CommonProxy {
	public ClientProxy() {
		CLagUtils.debug("CLag.ClientProxy constructor");
	}

	@Override
	public void registerTickHandler() {
		// needed here for "server" part on singleplayer 2x clientproxy, distinguished by Side.SERVER param to TickRegistry
		//CLagUtils.debug("CLag: ClientProxy.registerTickHandler 01");
		//TickRegistry.registerTickHandler(new CLagTickHandler(), Side.SERVER);
		//CLagUtils.debug("CLag: ClientProxy.registerTickHandler 02");
	}
}
