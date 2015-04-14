package jarcode.consoles.computer.interpreter;

import jarcode.consoles.computer.ProgramInstance;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.lib.IoLib;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class GameIoLib extends IoLib {

	private ProgramInstance inst;

	public GameIoLib(ProgramInstance inst) {
		this.inst = inst;
	}

	@Override
	protected File wrapStdin() throws IOException {
		return new WrappedFile(inst.stdin, inst.out);
	}

	@Override
	protected File wrapStdout() throws IOException {
		return new WrappedFile(inst.in, inst.stdout);
	}

	@Override
	protected File wrapStderr() throws IOException {
		return new WrappedFile(inst.in, inst.stdout);
	}

	@Override
	protected File openFile(String s, boolean b, boolean b1, boolean b2, boolean b3) throws IOException {
		throw new UnsupportedOperationException("Stahp");
	}

	@Override
	protected File tmpFile() throws IOException {
		throw new UnsupportedOperationException("Stahp");
	}

	@Override
	protected File openProgram(String s, String s1) throws IOException {
		throw new UnsupportedOperationException("Stahp");
	}


	private final class WrappedFile extends File {
		private InputStream in;
		private OutputStream out;
		private boolean closed = false;
		private Charset charset = Charset.forName("UTF-8");
		private WrappedFile(InputStream in, OutputStream out) {
			this.in = in;
			this.out = out;
		}

		public String tojstring() {
			return "file (" + this.hashCode() + ")";
		}

		public void write(LuaString str) throws IOException {
			out.write(str.checkjstring().getBytes(charset));
		}

		public void flush() throws IOException {
			out.flush();
		}

		public boolean isstdfile() {
			return true;
		}

		public void close() throws IOException {
			out.close();
			in.close();
			boolean closed = true;
		}

		public boolean isclosed() {
			return closed;
		}

		public int seek(String var1, int var2) throws IOException {
			throw new UnsupportedOperationException("seek() not supported");
		}

		public void setvbuf(String var1, int var2) {
			throw new UnsupportedOperationException("seek() not supported");
		}

		public int remaining() throws IOException {
			return in.available();
		}

		public int peek() throws IOException, EOFException {
			throw new UnsupportedOperationException("peek() not supported");
		}

		public int read() throws IOException, EOFException {
			return in.read();
		}

		public int read(byte[] var1, int var2, int var3) throws IOException {
			return in.read(var1, var2, var3);
		}
	}
}
