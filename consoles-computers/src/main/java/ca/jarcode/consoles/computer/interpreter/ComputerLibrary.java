package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptFunction;

import java.util.function.Supplier;

public class ComputerLibrary {

	public final boolean isRestricted;
	public final Supplier<NamedFunction[]> functions;
	public final String libraryName;

	public ComputerLibrary(String libraryName, boolean isRestricted, Supplier<NamedFunction[]> functions) {
		if (libraryName == null) {
			throw new NullPointerException("name cannot be null");
		}
		if (functions == null) {
			throw new NullPointerException("functions cannot be null");
		}
		this.isRestricted = isRestricted;
		this.functions = functions;
		this.libraryName = libraryName;
	}
	public static class NamedFunction {

		private String mappedName;
		public final ScriptFunction function;

		public NamedFunction(ScriptFunction function) {
			this.function = function;
		}
		public void setName(String name) {
			this.mappedName = name;
		}
		public String getMappedName() {
			return mappedName;
		}
	}
}
