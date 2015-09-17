package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.MapComponent;
import ca.jarcode.consoles.computer.filesystem.FSProvidedProgram;
import ca.jarcode.consoles.computer.manual.ProvidedManual;

import static ca.jarcode.consoles.computer.ProgramUtils.schedule;

@ProvidedManual(
		author = "Jarcode",
		version = "1.8",
		contents = "A program that allows you to navigate mapped area in the current world." +
				"The viewer is opened up in a separate screen " +
				"screen session. Commands are as follows:\n\n" +
				"\u00A7e/[number]\u00A7f sets the map scale\n" +
				"\u00A7e/-q\u00A7f quits the program\n"
)
public class MapProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {

		int x = computer.getConsole().getLocation().getBlockX();
		int y = computer.getConsole().getLocation().getBlockZ();

		if (!str.trim().isEmpty()) {
			String[] split = str.split(" ");
			x = Integer.parseInt(split[0]);
			y = Integer.parseInt(split[1]);
		}

		final int finalX = x;
		final int finalY = y;
		schedule(() -> {

			MapComponent component = new MapComponent(computer.getViewWidth(),
					computer.getViewHeight(), computer,
					finalX, finalY, 6);
			computer.setComponent(6, component);
			computer.switchView(7);
		});
	}
}
