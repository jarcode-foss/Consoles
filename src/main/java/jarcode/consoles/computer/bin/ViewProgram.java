package jarcode.consoles.computer.bin;

import jarcode.consoles.IndexedConsoleTextArea;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ViewProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {

		str = handleEncapsulation(str.trim());
		if (str.isEmpty()) {
			return;
		}
		Terminal terminal = computer.getTerminal(this);
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block == null) {
			println("view: " + str.trim() + ": no such file");
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
		schedule(() -> {
			IndexedConsoleTextArea component = new IndexedConsoleTextArea(computer.getViewWidth(),
					computer.getViewHeight(), computer.getConsole());
			component.setText(finalStr);
			computer.setComponent(7, component);
			computer.switchView(8);
		});
	}
}
