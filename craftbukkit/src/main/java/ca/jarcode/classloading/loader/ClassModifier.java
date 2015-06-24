package ca.jarcode.classloading.loader;

@FunctionalInterface
public interface ClassModifier {
	public byte[] instrument(byte[] in, String classname);
}
