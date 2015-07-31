package ca.jarcode.classloading.loader;

@FunctionalInterface
public interface ClassModifier {
	byte[] instrument(byte[] in, String classname);
}
