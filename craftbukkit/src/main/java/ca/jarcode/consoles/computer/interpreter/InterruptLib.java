package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.consoles.Consoles;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

import java.util.function.BooleanSupplier;

import static ca.jarcode.consoles.Lang.lang;

/*

This handles termination for Lua programs (as a library)

 */
public class InterruptLib extends DebugLib {

	private BooleanSupplier supplier;
	private volatile long lastInterrupt = System.currentTimeMillis();
	private byte off = 0;

	public InterruptLib(BooleanSupplier supplier) {
		this.supplier = supplier;
	}
	public void update() {
		lastInterrupt = System.currentTimeMillis();
	}
	@Override
	public void onInstruction(int i, Varargs varargs, int i1) {
		// we check every 20 instructions, less overhead this way.
		if (off == 20) {
			off = 0;
			// check if the program has been running too long without an interrupt
			if (System.currentTimeMillis() - lastInterrupt > Consoles.maxTimeWithoutInterrupt)
				throw new ProgramInterruptException(lang.getString("program-term"));
			// check if the program has been terminated
			if (supplier.getAsBoolean() || Lua.killAll) {
				throw new ProgramInterruptException(lang.getString("program-term"));
			}
		}
		off++;
		super.onInstruction(i, varargs, i1);
	}
}
