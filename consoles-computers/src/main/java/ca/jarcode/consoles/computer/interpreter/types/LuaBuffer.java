package ca.jarcode.consoles.computer.interpreter.types;

import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.computer.interpreter.BufferedFrameComponent;
import ca.jarcode.consoles.computer.interpreter.SandboxProgram;
import ca.jarcode.consoles.computer.manual.Arg;
import ca.jarcode.consoles.computer.manual.FunctionManual;
import ca.jarcode.consoles.computer.manual.TypeManual;

@TypeManual(
		value = "A screen buffer used to update the pixels of the screen " +
			"session that it is bound to. Used with LuaFrame to draw and update content.",
		usage = "-- Binds a new buffer to a screen session\n" +
				"local buffer = screenBuffer(index)\n" +
				"-- Switch to the session the buffer was created in\n" +
				"switchSession(index)")
@SuppressWarnings("unused")
public class LuaBuffer {

	private static final int MIN_UPDATE_TIME = 100;

	private SandboxProgram program;
	private BufferedFrameComponent component;
	private long lastUpdate = -1;
	private final int index;
	private Runnable update;

	public LuaBuffer(SandboxProgram program, int index, BufferedFrameComponent component, Runnable update) {
		this.program = program;
		this.component = component;
		this.index = index;
		this.update = update;
	}

	@FunctionManual("Updates this buffer with the given frame ID. The frame is removed (ie. LuaFrame:remove() " +
			"is called), and the operations performed on the frame will be visible on the screen session this " +
			"buffer is bound to.")
	public void update(
			@Arg(name = "id", info = "the id of the frame to use, retrieved with LuaFrame:id()") Integer id) {
		LuaFrame frame = program.framePool.get(id);
		if (System.currentTimeMillis() - lastUpdate >= MIN_UPDATE_TIME) {
			lastUpdate = System.currentTimeMillis();
		}
		else {
			try {
				Thread.sleep(MIN_UPDATE_TIME - (System.currentTimeMillis() - lastUpdate));
				update(id);
			}
			catch (InterruptedException e) {
				if (Computers.debug)
					e.printStackTrace();
			}
		}
		update.run();
		if (frame != null) {
			frame.remove();
			component.setOperations(frame.operations);
			frame.operations.clear();
		}
		else throw new IllegalArgumentException("Invalid frame");
	}

	@FunctionManual("Polls text input, returning a string if there is input read, otherwise returning nil. It is " +
			"advised to call this function until there is no more text to read.")
	public String poll() {
		return component.input();
	}

	@FunctionManual("Polls interaction input, returning a LuaInteraction if there is input to read, otherwise returning " +
			"nil. It is advised to call this function until there is no more interaction data to read.")
	public LuaInteraction pollCoords() {
		return component.interaction();
	}

	@FunctionManual("Destroys this buffer, cleaning up any resources and freeing the screen session it was bound to.")
	public void destroy() {
		program.getComputer().setComponent(index, null);
	}
}
