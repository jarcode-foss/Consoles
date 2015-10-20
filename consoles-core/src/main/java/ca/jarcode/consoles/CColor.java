package ca.jarcode.consoles;

public class CColor {

	public static String strip(String source) {
		StringBuilder builder = new StringBuilder();
		char[] arr = source.toCharArray();
		for (int t = 0; t < source.length(); t++) {
			if (arr[t] != '\u00A7' && (!colorCharRange(arr[t]) || t == 0 || arr[t - 1] != '\u00A7')) {
				builder.append(arr[t]);
			}
		}
		return builder.toString();
	}
	public static boolean colorCharRange(char c) {
		return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) != -1;
	}
}
