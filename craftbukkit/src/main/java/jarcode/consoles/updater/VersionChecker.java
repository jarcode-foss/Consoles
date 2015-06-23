package jarcode.consoles.updater;

import jarcode.consoles.Consoles;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Function;

import static jarcode.consoles.Lang.lang;

public class VersionChecker {

	private static final String PERMISSION = "consoles.update";
	private static final String MODULE = "jarcode.mc-consoles";
	private static final Function<String, String> URL_PROVIDER =
			(module) -> "http://jarcode.ca/versioncheck.php?m=" + module.replace(".", "_");

	public static void check() {

		String data = null;
		int response = 0;
		try {

			URL link = new URL(URL_PROVIDER.apply(MODULE));
			HttpURLConnection con = (HttpURLConnection) link.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			response = con.getResponseCode();
			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			data = IOUtils.toString(reader);
			reader.close();
		} catch (IOException e) {
			Consoles.getInstance().getLogger().warning(String.format(lang.getString("update-check-fail"), response));
			e.printStackTrace();
		}
		if (data != null) {
			data = data.replace("\n", "").trim();
			if (!Consoles.getInstance().getDescription().getVersion().equals(data)) {
				final String finalData = data;
				Bukkit.getOnlinePlayers().stream()
						.filter(player -> player.isOp() || player.hasPermission(PERMISSION)).forEach(player -> {
					player.sendMessage(ChatColor.YELLOW + String.format(lang.getString("update-available"), finalData));
					player.sendMessage("https://www.spigotmc.org/resources/consoles-api-mechanics.5804/");
				});
			}
		}
	}
}
