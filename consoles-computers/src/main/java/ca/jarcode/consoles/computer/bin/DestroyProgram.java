package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.filesystem.FSProvidedProgram;
import ca.jarcode.consoles.computer.manual.ProvidedManual;
import ca.jarcode.consoles.internal.ConsoleButton;
import ca.jarcode.consoles.api.Position2D;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static ca.jarcode.consoles.computer.ProgramUtils.schedule;

@ProvidedManual(
		author = "Jarcode",
		version = "1.3",
		contents = "Opens a dialog that presents the option to destroy (drop) the computer. " +
				"Only the owner of this computer can authorize this. Data is saved with the dropped " +
				"item."
)
public class DestroyProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		schedule(() -> {
			ConsoleButton delete = new ConsoleButton(computer.getConsole(), "Confirm");
			ConsoleButton deny = new ConsoleButton(computer.getConsole(), "Deny");
			Position2D pos = computer.dialog("Are you sure you want to delete this computer?", delete, deny);
			delete.addEventListener(event -> {
				Player player = event.getPlayer();
				if (computer.getOwner().equals(player.getUniqueId()))
					computer.destroy(false);
				else {
					player.sendMessage(ChatColor.YELLOW + "You have to be the owner to do that!");
					computer.getConsole().removeComponent(pos);
					computer.getConsole().repaint();
				}
			});
			deny.addEventListener(event -> {
				computer.getConsole().removeComponent(pos);
				computer.getConsole().repaint();
			});
		});
	}
}
