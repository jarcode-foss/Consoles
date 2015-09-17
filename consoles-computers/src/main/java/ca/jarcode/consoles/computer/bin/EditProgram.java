package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.EditorComponent;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.filesystem.FSBlock;
import ca.jarcode.consoles.computer.filesystem.FSFile;
import ca.jarcode.consoles.computer.filesystem.FSProvidedProgram;
import ca.jarcode.consoles.computer.manual.ProvidedManual;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import static ca.jarcode.consoles.computer.ProgramUtils.schedule;
import static ca.jarcode.consoles.computer.ProgramUtils.splitArguments;

@ProvidedManual(
		author = "Jarcode",
		version = "2.14",
		contents = "Opens up an editor for the given file. Navigation commands are prefixed " +
				"by the dash '-' character. The editor is opened up in a separate screen " +
				"screen session. Commands in the editor are as follows:\n\n" +
				"\u00A7e/-n\u00A7f adds a new line\n" +
				"\u00A7e/-q\u00A7f saves and quits\n" +
				"\u00A7e/-Q\u00A7f quits without saving\n" +
				"\u00A7e/-u\u00A7f scrolls up by two lines\n" +
				"\u00A7e/-d\u00A7f scrolls down by two lines\n" +
				"\u00A7e/-U\u00A7f scrolls to the top of the file\n" +
				"\u00A7e/-D\u00A7f scrolls to the bottom of the file\n" +
				"\u00A7e/-t\u00A7f adds four spaces (tab)"
)
public class EditProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		String[] args = splitArguments(str);
		if (args.length == 0 || str.isEmpty()) {
			println("edit [FILE] {INDEX}");
		}
		str = args[0];
		int index = -1;
		if (args.length >= 2) {
			try {
				index = Integer.parseInt(args[1]);
			}
			catch (Throwable e) {
				print("edit: " + str.trim() + ": invalid index");
				return;
			}
		}
		Terminal terminal = computer.getTerminal(this);
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block == null) {
			print("edit: " + str.trim() + ": no such file");
			return;
		}
		if (!(block instanceof FSFile)) {
			print("edit: " + str.trim() + ": not a file");
			return;
		}
		FSFile file = (FSFile) block;
		boolean printed = false;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Charset charset = Charset.forName("UTF-8");
		try (InputStream is = file.createInput()) {
			int i;
			while (true) {
				if (terminated())
					break;
				if (is.available() > 0 || is instanceof ByteArrayInputStream) {
					i = is.read();
					if (i == -1) break;
					out.write((byte) i);
				} else {
					if (!printed) {
						print("reading file, waiting for EOF or termination...");
						printed = true;
					}
					Thread.sleep(50);
				}
			}
			if (terminated())
				print("\tterminated");
		}
		str = new String(out.toByteArray(), charset);
		final String finalStr = str.replace("\r", "");
		final int finalIndex = index;
		schedule(() -> {
			EditorComponent component = new EditorComponent(computer.getViewWidth(),
					computer.getViewHeight(), computer, file, 7);
			component.setContent(finalStr);
			if (finalIndex != -1)
				component.setView(finalIndex);
			computer.setComponent(7, component);
			computer.switchView(8);
		});
	}
}
