package ca.jarcode.consoles;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static ca.jarcode.consoles.Lang.lang;

/*

Class used to compare versions at runtime

 */
public class Pkg {

	public static final List<Pkg> VERSIONS = new ArrayList<>();

	// supported NMS versions
	public static final Pkg v1_8_R2 = new Pkg("v1_8_R2");
	public static final Pkg v1_8_R3 = new Pkg("v1_8_R3");

	private static String VERSION = "?";

	public static void setVersion(String str) {
		VERSION = str;
	}

	public static boolean version(Pkg ver) {
		return VERSION.equals(ver.name);
	}

	public static void verify() {
		verify(VERSIONS.toArray(new Pkg[VERSIONS.size()]));
	}

	public static void verify(Pkg... versions) {
		for (Pkg ver : versions)
			if (ver.name.equals(VERSION))
				return;
		unsupported();
	}

	public static Field findField(Class source, Function<Pkg, String> finder) throws NoSuchFieldException {
		String name = finder.apply(forName(VERSION));
		Field field = source.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	public static Field findField(Class source, Object[][] map) throws NoSuchFieldException {
		String name = Arrays.asList(map).stream()
				.map((obj) -> {
					if (obj.length >= 2 && obj[0] instanceof Pkg && obj[1] instanceof String)
						return new AbstractMap.SimpleEntry<>((Pkg) obj[0], (String) obj[1]);
					else throw new IllegalArgumentException();
				})
				.filter((entry) -> entry.getKey() == forName(VERSION))
				.map(AbstractMap.SimpleEntry::getValue)
				.findFirst()
				.orElseGet(() -> null);
		if (name == null)
			unsupported();
		Field field = source.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private static void unsupported() {
		throw new RuntimeException(String.format(lang.getString("unsupported"), VERSION));
	}

	private static Pkg forName(String name) {
		for (Pkg p : VERSIONS)
			if (p.name.equals(name))
				return p;
		return null;
	}

	private final String name;

	private Pkg(String name) {
		this.name = name;
		VERSIONS.add(this);
	}
}
