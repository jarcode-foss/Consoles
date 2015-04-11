package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSGroup;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

public class CatProgram extends FSProvidedProgram {

	public CatProgram() {
		setExecutable(FSGroup.ALL, true);
	}

	@Override
	public void run(String str, Computer computer) throws Exception {
		String path = handleEncapsulation(str);
		FSBlock block = resolve(path);
		if (block == null) {
			print(str + ": not found");
			return;
		}
		if (!(block instanceof FSFile)) {
			print(str + ": not a file");
			return;
		}
		FSFile file = (FSFile) block;
		try (InputStream is = file.createInput()) {
			int i;
			Charset charset = Charset.forName("UTF-8");
			while (true) {
				if (terminated())
					break;
				if (is.available() > 0 || is instanceof ByteArrayInputStream) {
					i = is.read();
					if (i == -1) break;
					print(new String(new byte[]{(byte) i}, charset));
				} else Thread.sleep(50);
			}
			if (!terminated())
				print(" [EOF]");
			else
				print(" [TERMINATED]");
		}
	}
}
