package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSFile;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.BooleanSupplier;

public class LuaFile extends LuaBlock {
	private final FSFile file;
	private BooleanSupplier supplier;
	public LuaFile(FSFile file, String path, String cd, BooleanSupplier supplier, Computer computer) {
		super(file, path, cd, computer);
		this.file = file;
		this.supplier = supplier;
	}
	public boolean append(String text) {
		try (OutputStream out = file.getOutput()) {
			out.write(text.getBytes(Charset.forName("UTF-8")));
			return true;
		}
		catch (IOException e) {
			if (Consoles.DEBUG)
				e.printStackTrace();
		}
		return false;
	}
	public boolean write(String text) {
		try (OutputStream out = file.createOutput()) {
			out.write(text.getBytes(Charset.forName("UTF-8")));
			return true;
		}
		catch (IOException e) {
			if (Consoles.DEBUG)
				e.printStackTrace();
		}
		return false;
	}
	public String read() {
		try (InputStream is = file.createInput()) {
			int i;
			Charset charset = Charset.forName("UTF-8");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while (true) {
				if (supplier.getAsBoolean())
					break;
				if (is.available() > 0 || is instanceof ByteArrayInputStream) {
					i = is.read();
					if (i == -1) break;
					out.write(i);
				}
			}
			return new String(out.toByteArray(), charset);
		} catch (IOException e) {
			if (Consoles.DEBUG)
				e.printStackTrace();
		}
		return null;
	}
}
