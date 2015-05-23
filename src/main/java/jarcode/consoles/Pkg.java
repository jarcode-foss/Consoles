package jarcode.consoles;

/*

Class used to compare versions at runtime

 */
public class Pkg {
	public static String VERSION = "?";
	public static boolean is(String ver) {
		return VERSION.equals(ver);
	}
}
