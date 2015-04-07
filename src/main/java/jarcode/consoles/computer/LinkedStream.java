package jarcode.consoles.computer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class LinkedStream extends InputStream {

	byte[] buffer = new byte[0];
	public volatile boolean end = false;
	private List<Runnable> onClose = new ArrayList<>();

	@Override
	public int read() throws IOException {
		if (end) return -1;
		try {
			while (buffer.length == 0) {
				synchronized (this) {
					this.wait();
				}
			}
			synchronized (this) {
				byte b = buffer[0];
				advance();
				return b;
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		//TODO: handle this
		return -1;
	}
	public void end() {
		end = true;
	}
	public void add(int b) {
		synchronized (this) {
			if (b == - 1) {
				end();
				return;
			}
			byte[] old = buffer.clone();
			buffer = new byte[old.length + 1];
			System.arraycopy(old, 0, buffer, 0, old.length);
			buffer[b] = (byte) b;
			this.notify();
		}
	}
	public void registerCloseListener(Runnable runnable) {
		onClose.add(runnable);
	}
	@Override
	public void close() {
		onClose.forEach(Runnable::run);
	}
	private void advance() {
		synchronized (this) {
			byte[] old = buffer.clone();
			buffer = new byte[old.length - 1];
			System.arraycopy(old, 0, buffer, 0, old.length - 1);
		}
	}
	public OutputStream createOutput() {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				add(b);
			}
		};
	}
}
