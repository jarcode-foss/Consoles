package ca.jarcode.consoles.api.nms;

public abstract class ConsolesNMS {

	public static MapInternals mapInternals;
	public static PacketInternals packetInternals;
	public static GeneralInternals internals;
	public static CommandInternals commandInternals;

	public enum ProtocolDirection {
		OUT, IN
	}
}
