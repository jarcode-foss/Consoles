package jarcode.consoles.computer.bin;

import jarcode.consoles.ConsoleButton;
import jarcode.consoles.Position2D;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DeleteProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		schedule(() -> {
			ConsoleButton delete = new ConsoleButton(computer.getConsole(), "Confirm");
			ConsoleButton deny = new ConsoleButton(computer.getConsole(), "Deny");
			Position2D pos = computer.dialog("Are you sure you want to delete this computer?", delete, deny);
			delete.addEventListener(event -> {
				Player player = event.getPlayer();
				if (computer.getOwner().equals(player.getUniqueId()))
					computer.destroy();
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
