package ca.jarcode.consoles.v1_8_R2;

import ca.jarcode.consoles.api.nms.ConsolesNMS;

public class NMSModule {
	public static void link() {
		ConsolesNMS.mapInternals = MapInjector.IMPL;
		ConsolesNMS.packetInternals = new InternalPacketManager();
		ConsolesNMS.internals = new GeneralUtils();
		ConsolesNMS.commandInternals = new CommandBlockUtils();
	}
}
