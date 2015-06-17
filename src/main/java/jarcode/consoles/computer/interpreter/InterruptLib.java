package jarcode.consoles.computer.interpreter;

import jarcode.consoles.Consoles;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

import java.util.function.BooleanSupplier;

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
				throw new ProgramInterruptException("Program terminated (ran too long without interrupt)");
			// check if the program has been terminated
			if (supplier.getAsBoolean() || Lua.killAll) {
				throw new ProgramInterruptException("Program terminated");
			}
		}
		off++;
		super.onInstruction(i, varargs, i1);
	}
}
