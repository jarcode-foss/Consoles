package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.Consoles;
import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.ProgramUtils;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.filesystem.FSBlock;
import ca.jarcode.consoles.computer.filesystem.FSFolder;
import ca.jarcode.consoles.computer.filesystem.FSProvidedProgram;
import ca.jarcode.consoles.computer.filesystem.FSStoredFile;
import ca.jarcode.consoles.computer.manual.ProvidedManual;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ca.jarcode.consoles.computer.ProgramUtils.splitArguments;

@ProvidedManual(
		author = "Jarcode",
		version = "1.1",
		contents = "Downloads from an external URL and saves the data to a file. " +
				"Fails if the URL points to an invalid server or if the server returns " +
				"an error. The URL must point to an HTTP server."
)
public class WGetProgram extends FSProvidedProgram {

	public static final int CHUNK_SIZE = 1024 * Computers.wgetChunkSize;

	@Override
	public void run(String str, Computer computer) throws Exception {
		
		Terminal terminal = computer.getTerminal(this);
		String[] args = splitArguments(str);
		if (args.length < 2 || str.isEmpty()) {
			print("wget [URL] [FILE]");
			return;
		}
		ProgramUtils.PreparedBlock prep =
				ProgramUtils.handleBlockCreate(args[1], (s) -> print("wget: " + s), terminal, false);
		if (prep.err == null)
			invoke(args[0], prep.blockParent, prep.blockName, this::print, this::terminated, terminal);
	}
	public static int invoke(String path, FSFolder folder, String fileName,
	                          Consumer<String> messageHandler, BooleanSupplier terminated, Terminal terminal) {
		FSStoredFile file;
		OutputStream out = null;
		InputStream is = null;
		if (messageHandler != null)
			messageHandler.accept("Downloading...");
		try {
			URL url = new URL(path);
			URLConnection con = url.openConnection();
			con.setRequestProperty("Accept-Charset", "UTF-8");
			con.setRequestProperty("User-Agent", "Mozilla/5.0"); // fake user agent for sites
			is = con.getInputStream();
			file = new FSStoredFile(terminal.getComputer());
			out = file.createOutput();

			int t = 0;
			boolean d = false;
			byte[] buf = new byte[CHUNK_SIZE];
			int r = 0;
			while (true) {
				do {
					int read = is.read(buf, r, CHUNK_SIZE - r);
					if (read == -1)
						break;
					r += read;
				}
				while (r < CHUNK_SIZE);
				out.write(buf);
				r = 0;
				Thread.sleep(300);
				if (terminated.getAsBoolean()) {
					if (messageHandler != null)
						messageHandler.accept("Terminated.");
					break;
				}
				t++;
				d = !d;
				if (d) {
					terminal.clear();
					if (messageHandler != null)
						messageHandler.accept("\ndownloaded: " + (t * CHUNK_SIZE) + " bytes");
				}
			}
		}
		catch (MalformedURLException e) {
			if (Consoles.debug)
				e.printStackTrace();
			if (messageHandler != null)
				messageHandler.accept("wget: " + path + ": malformed URL");
			return -1;
		}
		catch (Throwable e) {
			if (Consoles.debug)
				e.printStackTrace();
			if (messageHandler != null)
				messageHandler.accept("wget: " + path + ": failed to download, " + e.getClass().getSimpleName());
			return -2;
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
		if (messageHandler != null)
			messageHandler.accept("\nsaved to: " + fileName);
		folder.contents.put(fileName, file);
		return 0;
	}
}
