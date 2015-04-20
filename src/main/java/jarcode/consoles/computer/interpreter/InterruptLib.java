package jarcode.consoles.computer.interpreter;

import jarcode.consoles.Consoles;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

import java.util.function.BooleanSupplier;

public class InterruptLib extends DebugLib {

	private BooleanSupplier supplier;
	private final int max;
	private volatile long lastInterrupt = System.currentTimeMillis();
	private byte off = 0;

	public InterruptLib(BooleanSupplier supplier) {
		this.supplier = supplier;
		this.max = Consoles.maxTimeWithoutInterrupt;
	}
	public void update() {
		lastInterrupt = System.currentTimeMillis();
	}
	@Override
	public void onInstruction(int i, Varargs varargs, int i1) {
		if (off == 20) {
			off = 0;
			if (System.currentTimeMillis() - lastInterrupt > max)
				throw new ProgramInterruptException("Program terminated (ran too long without interrupt)");
		}
		off++;
		if (supplier.getAsBoolean()) {
			throw new ProgramInterruptException("Program terminated");
		}
		super.onInstruction(i, varargs, i1);
	}
}
