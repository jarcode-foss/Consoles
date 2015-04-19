package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.interpreter.BufferedFrameComponent;
import jarcode.consoles.computer.interpreter.InterpretedProgram;

public class LuaBuffer {

	private InterpretedProgram program;
	private BufferedFrameComponent component;
	private final int index;

	public LuaBuffer(InterpretedProgram program, int index, BufferedFrameComponent component) {
		this.program = program;
		this.component = component;
		this.index = index;
	}

	public void update(Integer id) {
		LuaFrame frame = program.framePool.remove(id);
		if (frame != null) {
			component.addOperations(frame.operations);
		}
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
