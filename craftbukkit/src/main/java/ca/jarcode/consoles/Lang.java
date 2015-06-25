package ca.jarcode.consoles;

import java.util.ResourceBundle;
import java.util.logging.Logger;

public class Lang {
	public static final ResourceBundle lang;
	static {
		try {
			lang = ResourceBundle.getBundle("ca.jarcode.consoles");
		}
		catch (Throwable e) {
			Logger.getGlobal().severe("Failed to load resource bundle: ");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
