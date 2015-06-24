package ca.jarcode.consoles.computer.interpreter.types;

import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.manual.Arg;
import ca.jarcode.consoles.computer.manual.FunctionManual;
import ca.jarcode.consoles.computer.manual.TypeManual;

@TypeManual(
		value = "Represents the terminal that the current program is running in.",
		usage = "-- Retrieve terminal that the program is running in\n" +
				"local term = getTerminal()")
public class LuaTerminal {

	private Terminal terminal;

	public LuaTerminal(Terminal terminal) {
		this.terminal = terminal;
	}

	@FunctionManual("Sets the prompt formatter for the terminal. %d is used as the directory variable, %h for " +
			"the hostname, and %u for the user.")
	public void setPrompt(
			@Arg(name = "formatter", info = "the formatter to use for the prompt") String prompt) {
		terminal.setPromptFormatter(prompt);
	}

	@FunctionManual("Returns a random joke from this terminal. Jokes are not guaranteed to be funny.")
	public String randomJoke() {
		return terminal.randomJoke();
	}

	@FunctionManual("Returns the user of this terminal")
	public String getUser() {
		return terminal.getUser();
	}
}
