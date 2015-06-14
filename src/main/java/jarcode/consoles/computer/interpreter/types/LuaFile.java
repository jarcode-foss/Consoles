package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.manual.Arg;
import jarcode.consoles.computer.manual.FunctionManual;
import jarcode.consoles.computer.manual.TypeManual;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.BooleanSupplier;

@TypeManual("A stored file that exists in the filesystem.")
@SuppressWarnings("unused")
public class LuaFile extends LuaBlock {
	private final FSFile file;
	private final BooleanSupplier supplier;
	public LuaFile(FSFile file, String path, String cd, BooleanSupplier supplier, Computer computer) {
		super(file, path, cd, computer);
		this.file = file;
		this.supplier = supplier;
	}
	@FunctionManual("Appends text to the end of the file")
	public boolean append(
			@Arg(name = "text", info = "the text to append to the file") String text) {
		try (OutputStream out = file.getOutput()) {
			out.write(text.getBytes(Charset.forName("UTF-8")));
			return true;
		}
		catch (IOException e) {
			if (Consoles.debug)
				e.printStackTrace();
		}
		return false;
	}

	@FunctionManual("Writes the given text to the file, wiping all previous contents.")
	public boolean write(
			@Arg(name = "text", info = "the text to write to the file") String text) {
		try (OutputStream out = file.createOutput()) {
			out.write(text.getBytes(Charset.forName("UTF-8")));
			return true;
		}
		catch (IOException e) {
			if (Consoles.debug)
				e.printStackTrace();
		}
		return false;
	}

	@FunctionManual("Reads all of this file's contents into a byte array. The LuaFile:read() function should " +
			"be used over this to conserve memory.")
	public byte[] data() {
		try (InputStream is = file.createInput()) {
			int i;
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
			out.toByteArray();
		} catch (IOException e) {
			if (Consoles.debug)
				e.printStackTrace();
		}
		return null;
	}

	@FunctionManual("Reads all of this file's contents into a string. The program will block until the file " +
			"is fully read.")
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
			if (Consoles.debug)
				e.printStackTrace();
		}
		return null;
	}
}
