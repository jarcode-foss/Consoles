package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.ascript.Script;
import ca.jarcode.ascript.interfaces.FuncPool;
import ca.jarcode.consoles.computer.Computer;

import java.util.function.BooleanSupplier;

@SuppressWarnings("unchecked")
public class ScriptContext {

	public static Computer getComputer() {
		return ((FuncPool<SandboxProgram>) Script.contextPool()).getUserdatum().getComputer();
	}
	public static BooleanSupplier terminatedSupplier() {
		return ((FuncPool<SandboxProgram>) Script.contextPool()).getUserdatum().terminated;
	}
}
