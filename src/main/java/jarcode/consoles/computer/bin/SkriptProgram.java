package jarcode.consoles.computer.bin;

import jarcode.consoles.internal.ConsoleButton;
import jarcode.consoles.util.Position2D;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.skript.ScriptInterface;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import static jarcode.consoles.computer.ProgramUtils.*;

@Manual(
		author = "Jarcode",
		version = "1.0",
		contents = "Uploads the contents of a file to Skript, if the plugin exists. This action " +
				"will need to be authorized by a user with the permission \u00A7ecomputer.skript.upload\n\n" +
				"The program will need to be provided with a file in the computer (with the skript), and an " +
				"identifier for the script. If a script already exists with the given identifier, it will be " +
				"overwritten."
)
@SuppressWarnings("SpellCheckingInspection")

public class SkriptProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		String[] args = splitArguments(str);
		if (args.length <= 1 || str.isEmpty()) {
			print("skript [FILE] [IDENTIFIER]");
			return;
		}
		str = args[0];
		if (!FSBlock.allowedBlockName(args[1])) {
			print("skript: illegal indetifier: " + args[1]);
			return;
		}
		Terminal terminal = computer.getTerminal(this);
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block == null) {
			print("skript: " + str.trim() + ": no such file");
			return;
		}
		if (!(block instanceof FSFile)) {
			print("skript: " + str.trim() + ": not a file");
			return;
		}
		FSFile file = (FSFile) block;
		boolean printed = false;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Charset charset = Charset.forName("UTF-8");
		try (InputStream is = file.createInput()) {
			int i;
			while (true) {
				if (terminated())
					break;
				if (is.available() > 0 || is instanceof ByteArrayInputStream) {
					i = is.read();
					if (i == -1) break;
					out.write((byte) i);
				} else {
					if (!printed) {
						print("reading file, waiting for EOF or termination...");
						printed = true;
					}
					Thread.sleep(50);
				}
			}
			if (terminated())
				print("\tterminated");
		}
		str = new String(out.toByteArray(), charset);
		String fs = str;
		AtomicInteger result = new AtomicInteger(0);
		if (!ScriptInterface.HOOK.isHooked()) {
			print("skript: could not find the script plugin");
		}
		main((resume) -> {
			ConsoleButton delete = new ConsoleButton(computer.getConsole(), "Upload");
			ConsoleButton deny = new ConsoleButton(computer.getConsole(), "Cancel");
			Position2D pos = computer.dialog(
					"This program will upload " + args[0] + " to Skript\n" +
					"You must have the permission computer.skript.upload to continue.",
					delete, deny);
			delete.addEventListener(event -> {
				Player player = event.getPlayer();
				if (player.hasPermission("computer.skript.upload")) {
					ScriptInterface.HOOK.upload(fs, args[1]);
					result.set(1);
				}
				resume.run();
			});
			deny.addEventListener(event -> {
				computer.getConsole().removeComponent(pos);
				computer.getConsole().repaint();
				result.set(2);
				resume.run();
			});
		}, this::terminated);
		switch (result.get()) {
			case 0:
				print("skript: insufficient permissions");
				break;
			case 1:
				print("skript: uploaded skript");
				break;
			case 2:
				print("skript: exiting without taking any action");
				break;
		}
	}
}
