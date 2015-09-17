package ca.jarcode.consoles.computer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/*

Input stream that can produce output streams that write to it.

This needs to be optimized into an n-dimensional array, or something. Having
the ability to re-allocate memory at specific addressed is something I really
miss about programming in C.

 */
public class LinkedStream extends InputStream {

	byte[] buffer = new byte[0];
	public volatile boolean end = false;
	private List<Runnable> onClose = new ArrayList<>();

	@Override
	public int read() throws IOException {
		try {
			while (!end && buffer.length == 0) {
				synchronized (this) {
					this.wait();
				}
			}
			if (end && buffer.length == 0) return -1;
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
				this.notify();
				return;
			}
			byte[] old = buffer.clone();
			buffer = new byte[old.length + 1];
			System.arraycopy(old, 0, buffer, 0, old.length);
			buffer[old.length] = (byte) b;
			this.notify();
		}
	}
	public void registerCloseListener(Runnable runnable) {
		onClose.add(runnable);
	}
	@Override
	public synchronized int available() {
		return buffer.length + (end ? 1 : 0);
	}
	@Override
	public void close() {
		onClose.forEach(Runnable::run);
	}
	private void advance() {
		synchronized (this) {
			byte[] old = buffer.clone();
			buffer = new byte[old.length - 1];
			System.arraycopy(old, 1, buffer, 0, old.length - 1);
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
