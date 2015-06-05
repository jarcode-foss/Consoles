package jarcode.consoles.computer.bin;

import jarcode.consoles.internal.IndexedConsoleTextArea;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import static jarcode.consoles.computer.ProgramUtils.*;

@Manual(
		author = "Jarcode",
		version = "1.2",
		contents = "Displays the contents of a file on a separate screen instance. The program " +
				"exists immediately, but allows the screen session to remain afterwards."
)
public class ViewProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		String[] args = splitArguments(str);
		if (args.length == 0 || str.isEmpty()) {
			println("view [FILE] {INDEX}");
		}
		str = args[0];
		int index = -1;
		if (args.length >= 2) {
			try {
				index = Integer.parseInt(args[1]);
			}
			catch (Throwable e) {
				print("view: " + str.trim() + ": invalid index");
				return;
			}
		}
		Terminal terminal = computer.getTerminal(this);
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block == null) {
			print("view: " + str.trim() + ": no such file");
			return;
		}
		if (!(block instanceof FSFile)) {
			print("view: " + str.trim() + ": not a file");
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
		final String finalStr = str;
		final int finalIndex = index;
		schedule(() -> {
			IndexedConsoleTextArea component = new IndexedConsoleTextArea(computer.getViewWidth(),
					computer.getViewHeight(), computer.getConsole());
			if (finalIndex == -1)
				component.setText(finalStr);
			else
				component.setText(finalStr, finalIndex);
			computer.setComponent(7, component);
			computer.switchView(8);
		});
	}
}
