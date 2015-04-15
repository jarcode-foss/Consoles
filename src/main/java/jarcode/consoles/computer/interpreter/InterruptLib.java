package jarcode.consoles.computer.interpreter;

import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

public class InterruptLib extends DebugLib {
	private volatile boolean terminated = false;

	@Override
	public void onInstruction(int i, Varargs varargs, int i1) {
		if (terminated) {
			throw new ProgramInterruptException("Program terminated");
		}
		super.onInstruction(i, varargs, i1);
	}
	public void terminate() {
		terminated = true;
	}
}
