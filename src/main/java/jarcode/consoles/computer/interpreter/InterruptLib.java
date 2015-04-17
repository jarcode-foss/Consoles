package jarcode.consoles.computer.interpreter;

import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

import java.util.function.BooleanSupplier;

public class InterruptLib extends DebugLib {

	private BooleanSupplier supplier;

	public InterruptLib(BooleanSupplier supplier) {
		this.supplier = supplier;
	}

	@Override
	public void onInstruction(int i, Varargs varargs, int i1) {
		if (supplier.getAsBoolean()) {
			throw new ProgramInterruptException("Program terminated");
		}
		super.onInstruction(i, varargs, i1);
	}
}
