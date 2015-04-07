package jarcode.consoles;

public class SimpleConsole extends Console {

	private ConsoleTextArea area;

	public SimpleConsole(int w, int h) {
		super(w, h);
		area = ConsoleTextArea.createOver(this);
		area.placeOver(this);
	}
	public void print(String text) {
		area.print(text);
	}
	public void println(String text) {
		area.println(text);
	}
}
