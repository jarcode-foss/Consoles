package jarcode.consoles.command;

import jarcode.consoles.ConsoleHandler;
import jarcode.consoles.Consoles;
import jarcode.consoles.computer.ComputerHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

// this is a really stripped-down version of a class loader I normally use in private code,
// so if you're cringing at my wheel-reinventing, this normally used custom class loaders
// and was being used for my own plugin implementations. I'm just copying code - if you
// want to reformat everything to use the bukkit API - make a PR.
public class CommandHandler implements Listener {

	final HashMap<String, CommandBase> commands = new LinkedHashMap<>();

	final Class<?> BASE = CommandBase.class;

	public CommandHandler(Class... defaults) {

		if (Consoles.getInstance() == null) {
			throw new RuntimeException("Main plugin has not been instantiated yet!");
		}

		for (Class<?> commandClass : defaults) {
			try {
				if (handleClass(commandClass))
					Consoles.getInstance().getLogger().info("Loaded command"
							+ ": " + commandClass.getSimpleName());
			} catch (Exception e) {
				e.printStackTrace();
				Consoles.getInstance()
						.getLogger()
						.severe("Could not load default command: " + commandClass.getSimpleName() + ", " + e.toString());
			}
		}

	}

	private boolean handleClass(Class<?> commandClass) throws IllegalAccessException, InstantiationException {

		if (BASE.isAssignableFrom(commandClass)) {
			if (canCreateInstance(commandClass) && hasEmptyConstructor(commandClass)) {
				Object instance = commandClass.newInstance();
				CommandBase cmd = (CommandBase) instance;
				cmd.setCommandHandler(this);
				for (String name : cmd.getNames()) {
					commands.put(name, cmd);
				}
				return true;
			}
		}
		return false;
	}

	public void addCommand(Class<? extends CommandBase> commandClass) throws IllegalAccessException, InstantiationException {
		if (canCreateInstance(commandClass) && hasEmptyConstructor(commandClass)) {
			Object instance = commandClass.newInstance();
			CommandBase cmd = (CommandBase) instance;

			for (String name : cmd.getNames()) {
				commands.put(name, cmd);
			}
		}
	}

	private boolean hasEmptyConstructor(Class<?> targetClass) {
		for (Constructor con : targetClass.getConstructors()) {
			if (con.getParameterTypes().length == 0) return true;
		}
		return false;
	}

	private boolean canCreateInstance(Class<?> targetClass) {
		return !targetClass.isInterface() && !targetClass.isEnum()
				&& !targetClass.isAnnotation() && !targetClass.isArray()
				&& !Modifier.isAbstract(targetClass.getModifiers());
	}

	private boolean handle(CommandSender sender, String name, String message, String[] args) {
		try {
			return commands.get(name.toLowerCase()).onCommand(sender, name, message, args);
		} catch (Throwable t) {
			sender.sendMessage(ChatColor.RED + "Failed to execute \"" + name + "\": " + t.getMessage());
			t.printStackTrace();
		}
		return false;
	}

	public HashMap<String, CommandBase> getCommandMappings() {
		return commands;
	}

	@EventHandler
	@SuppressWarnings("unused")
	void onPreCommand(PlayerCommandPreprocessEvent e) {

		if (e.getMessage().startsWith("/") && !e.getMessage().substring(1).trim().isEmpty()) {
			ComputerHandler handler = ComputerHandler.getInstance();
			if (handler != null && ConsoleHandler.getInstance().hittingConsole(e.getPlayer())) {
				handler.command(e.getMessage().substring(1), e.getPlayer());
				e.setCancelled(true);
				return;
			}
		}

		if (e.getMessage().startsWith("//")) {
			return;
		}

		String cmdName; // obtain command name
		if (!e.getMessage().contains(" ")) {
			cmdName = e.getMessage().substring(1);
		} else {
			cmdName = e.getMessage().substring(1, e.getMessage().indexOf(" "));
		}
		String message = e.getMessage().substring(1); // message without the '/'
		String[] args = message.contains(" ") ? message.substring(message.indexOf(" ") + 1).trim().split(" ") : new String[0]; // split command arguments
		CommandBase command = commands.get(cmdName.toLowerCase());

		if (command == null) return;
		if (command.hasNode(e.getPlayer())) {
			boolean result = handle(e.getPlayer(), cmdName, message, args);
			if (!result) {
				// dispatch monkeys
				e.getPlayer().sendMessage(new String[] {ChatColor.RED + "An error occurred while executing command: " + cmdName,
						ChatColor.RED + "A squad of highly trained monkeys have been dispatched to your location to fix the problem.",
						ChatColor.RED + "If you see them, give this this code: ",
						"VGhlIGNha2UgaXMgYSBsaWU="});
				dispatchMonkeys(e.getPlayer().getAddress());
			}
		}
		else {
			e.getPlayer().sendMessage(ChatColor.YELLOW + "You can't use that command!");
		}
		if (command.cancelAfter())
			e.setCancelled(true);
	}
	@EventHandler
	@SuppressWarnings("unused")
	void onConsoleCommand(ServerCommandEvent e) {
		String cmdName; // obtain command name
		if (!e.getCommand().contains(" ")) {
			cmdName = e.getCommand();
		} else {
			cmdName = e.getCommand().substring(0, e.getCommand().indexOf(" "));
		}
		String message = e.getCommand();// message without the '/'
		String[] args = message.contains(" ") ? message.substring(message.indexOf(" ") + 1).trim().split(" ") : new String[0]; // split command arguments
		CommandBase command = commands.get(cmdName.toLowerCase());
		if (command == null) return;
		if (!handle(e.getSender(), cmdName, message, args))
			e.getSender().sendMessage("Something went wrong while processing that command.");
		e.setCommand("/");
	}
	@SuppressWarnings("unused")
	public void dispatchMonkeys(InetSocketAddress address) {
		// TODO: add monkey dispatching functionality
	}
}
