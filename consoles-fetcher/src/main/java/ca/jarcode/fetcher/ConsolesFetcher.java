package ca.jarcode.fetcher;

/**
 * This class contains functionality to grab consoles modules from a maven repository.
 *
 * You may use this class in any piece of code, for any purpose, without any restrictions. You are free to modify and
 * distribute with only the request (not obligation) that you release the source.
 *
 * @author Jarcode
 *
 */
public class ConsolesFetcher {

	private static final String GROUP_ID, ARTIFACT_ID, REPOSITORY_URL;

	static {
		GROUP_ID = "ca.jarcode";
		ARTIFACT_ID = "consoles-core";
		REPOSITORY_URL = "http://jarcode.ca/maven2";
	}

	public static void fetchPlugin() {

	}
}
