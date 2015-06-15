package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.manual.ProvidedManual;
import jarcode.consoles.internal.ConsoleButton;
import jarcode.consoles.util.Position2D;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static jarcode.consoles.computer.ProgramUtils.*;

@ProvidedManual(
		author = "Jarcode",
		version = "1.3",
		contents = "Opens a dialog that presents the option to delete the computer. " +
				"Only the owner of this computer can authorize deletion. All data is " +
				"lost when the computer is deleted."
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
					computer.destroy(true);
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
