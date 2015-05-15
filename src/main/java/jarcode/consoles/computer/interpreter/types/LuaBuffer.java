package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.interpreter.BufferedFrameComponent;
import jarcode.consoles.computer.interpreter.InterpretedProgram;

public class LuaBuffer {

	private static final int MIN_UPDATE_TIME = 100;

	private InterpretedProgram program;
	private BufferedFrameComponent component;
	private long lastUpdate = -1;
	private final int index;
	private Runnable update;

	public LuaBuffer(InterpretedProgram program, int index, BufferedFrameComponent component, Runnable update) {
		this.program = program;
		this.component = component;
		this.index = index;
		this.update = update;
	}

	public void update(Integer id) {
		LuaFrame frame = program.framePool.remove(id);
		if (System.currentTimeMillis() - lastUpdate >= MIN_UPDATE_TIME) {
			lastUpdate = System.currentTimeMillis();
		}
		else {
			try {
				Thread.sleep(MIN_UPDATE_TIME - (System.currentTimeMillis() - lastUpdate));
				update(id);
			}
			catch (InterruptedException e) {
				if (Consoles.DEBUG)
					e.printStackTrace();
			}
		}
		update.run();
		if (frame != null) {
			component.setOperations(frame.operations);
			frame.operations.clear();
		}
		else throw new IllegalArgumentException("Invalid frame");
	}

	public String poll() {
		return component.input();
	}

	public LuaInteraction pollCoords() {
		return component.interaction();
	}

	public void destroy() {
		program.getComputer().setComponent(index, null);
	}
}
