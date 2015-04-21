package jarcode.consoles.computer.bin;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.filesystem.FSStoredFile;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.stream.Collectors;

@Manual(
		author = "Jarcode",
		version = "1.1",
		contents = "Downloads from an external URL and saves the data to a file. " +
				"Fails if the URL points to an invalid server or if the server returns " +
				"an error. The URL must point to an HTTP server."
)
public class WGetProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		
		Terminal terminal = computer.getTerminal(this);
		String[] args = splitArguments(str);
		if (args.length < 2 || str.isEmpty()) {
			print("wget [URL] [FILE]");
			return;
		}
		str = args[1];
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block != null) {
			print("wget: " + str.trim() + ": file or folder exists");
			return;
		}
		block = computer.getBlock("", terminal.getCurrentDirectory());
		if (!(block instanceof FSFolder)) {
			print("wget: " + str.trim() + ": invalid current directory");
			return;
		}
		String[] arr = FSBlock.section(str, "/");
		String f = Arrays.asList(arr).stream()
				.limit(arr.length == 0 ? 0 : arr.length - 1)
				.collect(Collectors.joining("/"));
		if (f.trim().isEmpty() && str.startsWith("/"))
			f = "/";
		String n = Arrays.asList(arr).stream()
				.filter(s -> !s.isEmpty())
				.reduce((o1, o2) -> o2)
				.get();
		if (!FSBlock.allowedBlockName(n)) {
			print("wget: " + n.trim() + ": bad block name");
			return;
		}
		FSBlock folder = computer.getBlock(f, terminal.getCurrentDirectory());
		if (folder == null) {
			print("wget: " + f.trim() + ": does not exist");
			return;
		}
		if (!(folder instanceof FSFolder)) {
			print("wget: " + f.trim() + ": not a folder");
			return;
		}
		FSStoredFile file;
		OutputStream out = null;
		InputStream is = null;
		try {
			URL url = new URL(args[0]);
			URLConnection con = url.openConnection();
			con.setRequestProperty("Accept-Charset", "UTF-8");
			con.setRequestProperty("User-Agent", "Mozilla/5.0"); // fake user agent for sites
			is = con.getInputStream();
			file = new FSStoredFile();
			out = file.createOutput();
			IOUtils.copy(is, out);
		}
		catch (MalformedURLException e) {
			if (Consoles.DEBUG)
				e.printStackTrace();
			print("wget: " + args[0] + ": malformed URL");
			return;
		}
		catch (Throwable e) {
			if (Consoles.DEBUG)
				e.printStackTrace();
			print("wget: " + args[0] + ": failed to download, " + e.getClass().getSimpleName());
			return;
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException ignored) {}
			}
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException ignored) {}
			}
		}
		((FSFolder) folder).contents.put(n, file);
	}
}
