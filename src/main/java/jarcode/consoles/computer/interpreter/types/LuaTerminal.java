package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.manual.Arg;
import jarcode.consoles.computer.manual.FunctionManual;

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
