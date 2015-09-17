package ca.jarcode.consoles.computer.skript;

import java.util.function.Consumer;

// hahahaha this class is so bad

// I hope whoever reads this barfs on their keyboard
// I did this on purpose to see how much functionality you can
// stuff into a Java 8 interface.

@SuppressWarnings("unchecked")
public interface ScriptInterface {

	Object[] FUNCTIONS = new Object[1];

	ScriptInterface HOOK = new ScriptInterface() {

		private ScriptInterface underlying = new ScriptInterface() {};

		{
			FUNCTIONS[0] = (Consumer<ScriptInterface>) (inst) -> underlying = inst;
		}

		@Override
		public boolean isHooked() {
			return underlying.isHooked();
		}

		@Override
		public void upload(String script, String identifier) {
			underlying.upload(script, identifier);
		}
	};

	static void set(ScriptInterface inst) {
		((Consumer<ScriptInterface>) FUNCTIONS[0]).accept(inst);
	}

	default boolean isHooked() {
		return false;
	}

	default void upload(String script, String identifier)  {}

	@SuppressWarnings("UnusedDeclaration")
	class FailedHookException extends Exception {
		public FailedHookException(String message) {
			super(message);
		}
		public FailedHookException(String message, Throwable cause) {
			super(message, cause);
		}
		public FailedHookException(Throwable cause) {
			super(cause);
		}
		public FailedHookException() {
			super();
		}
	}
}
