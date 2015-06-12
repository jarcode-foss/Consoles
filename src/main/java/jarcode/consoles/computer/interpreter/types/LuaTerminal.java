package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.Terminal;

public class LuaTerminal {

	private Terminal terminal;

	public LuaTerminal(Terminal terminal) {
		this.terminal = terminal;
	}

	public void setPrompt(String prompt) {
		terminal.setPromptFormatter(prompt);
	}

	public void printRandomJoke() {
		terminal.randomJoke();
	}

	public String getUser() {
		return terminal.getUser();
	}
}
