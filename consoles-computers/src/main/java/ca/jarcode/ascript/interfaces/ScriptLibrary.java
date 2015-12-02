package ca.jarcode.ascript.interfaces;

import java.util.function.Function;
import java.util.function.Supplier;

public class ScriptLibrary {

	public final boolean isRestricted;
	public final Supplier<NamedFunction[]> functions;
	public final String libraryName;

	public ScriptLibrary(String libraryName, boolean isRestricted, Supplier<NamedFunction[]> functions) {
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
		public final Function<ScriptGlobals, ScriptFunction> preparedFunction;

		public NamedFunction(Function<ScriptGlobals, ScriptFunction> function) {
			this.preparedFunction = function;
		}
		public void setName(String name) {
			this.mappedName = name;
		}
		public String getMappedName() {
			return mappedName;
		}
	}
}
