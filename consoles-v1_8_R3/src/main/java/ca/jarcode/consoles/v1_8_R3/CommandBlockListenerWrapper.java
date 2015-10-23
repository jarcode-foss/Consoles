package ca.jarcode.consoles.v1_8_R3;

import ca.jarcode.consoles.api.nms.CommandExecutor;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

/*

Wrapper class for the NMS command block listener that we use to
listen for changes and events from the command block.

 */
public class CommandBlockListenerWrapper extends CommandBlockListenerAbstract {

	private static final Field COMMAND_RESULT, CHAT_COMPONENT, SENDER;

	private static final String[] OVERRIDE_COMMANDS = {
			"link"
	};

	private CommandExecutor consoleListener;
	private BooleanSupplier commandBlocksEnabled;

	static {
		try {

			COMMAND_RESULT = CommandBlockListenerAbstract.class.getDeclaredField("b");
			CHAT_COMPONENT = CommandBlockListenerAbstract.class.getDeclaredField("d");
			SENDER = CommandBlockListenerAbstract.class.getDeclaredField("sender");
			COMMAND_RESULT.setAccessible(true);
			CHAT_COMPONENT.setAccessible(true);
			SENDER.setAccessible(true);
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private void setResult(int result) {
		try {
			COMMAND_RESULT.setInt(this, result);
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private void setChatComponent(Object obj) {
		try {
			CHAT_COMPONENT.set(this, obj);
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static CommandSender getSender(CommandBlockListenerAbstract inst) {
		try {
			return (CommandSender) SENDER.get(inst);
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}


	final CommandBlockListenerAbstract underlying;
	// this is a cheat to make this act like an inner class when we get this$0 via reflection later
	@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
	private final TileEntityCommand this$0;

	CommandBlockListenerWrapper(CommandBlockListenerAbstract underlying,
	                            BooleanSupplier commandBlocksEnabled, TileEntityCommand command) {
		this.underlying = underlying;
		this.commandBlocksEnabled = commandBlocksEnabled;
		this.sender = getSender(underlying);
		this.this$0 = command;
	}

	public void setConsoleListener(CommandExecutor listener) {
		consoleListener = listener;
	}

	public boolean listening() {
		return consoleListener != null;
	}

	private boolean override() {
		String command = getCommand().toLowerCase();
		if(command.startsWith("/")) {
			command = command.substring(1);
		}
		command = command.split(" ")[0];
		return Arrays.asList(OVERRIDE_COMMANDS).contains(command);
	}

	// -- override methods --
	// (we can't use @Override for version reasons)

	public int j() {
		return underlying.j();
	}

	public IChatBaseComponent k() {
		return underlying.k();
	}

	public void a(NBTTagCompound nbttagcompound) {
		underlying.a(nbttagcompound);
	}

	public void b(NBTTagCompound nbttagcompound) {
		underlying.b(nbttagcompound);
	}

	public boolean a(int i, String s) {
		return underlying.a(i, s);
	}

	public void setCommand(String s) {
		underlying.setCommand(s);
	}

	public String getCommand() {
		return underlying.getCommand();
	}

	public void a(World world) {
		if (consoleListener != null) {
			sendMessage(new ChatComponentText(consoleListener.execute(sender, getCommand())));
			setResult(0);
			return;
		}
		if (!commandBlocksEnabled.getAsBoolean() && !override()) {
			setChatComponent(new ChatComponentText("You cannot use server commands"));
			h();
			setResult(0);
			return;
		}
		if(world.isClientSide) {
			setResult(0);
		}
		MinecraftServer minecraftserver = MinecraftServer.getServer();
		if(minecraftserver != null && underlyingCheck()) {
			minecraftserver.getCommandHandler();
			try {
				setChatComponent(null);
				setResult(executeCommand(this, sender, getCommand()));
			} catch (Throwable var6) {
				var6.printStackTrace();
			}
		} else {
			setResult(0);
		}
	}

	// I'm not sure what this method is for, but it's a check when handling vanilla commands
	// in the minecraft server. It changed in the last version, so I'm just adding this fix.
	public boolean underlyingCheck() {
		// this is "N" in previous version
		String name = "O";
		try {
			Method method = MinecraftServer.class.getMethod(name);
			return (Boolean) method.invoke(MinecraftServer.getServer());
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public String getName() {
		return underlying.getName();
	}

	public IChatBaseComponent getScoreboardDisplayName() {
		return underlying.getScoreboardDisplayName();
	}

	public void setName(String s) {
		underlying.setName(s);
	}

	public void sendMessage(IChatBaseComponent ichatbasecomponent) {
		underlying.sendMessage(ichatbasecomponent);
	}

	public boolean getSendCommandFeedback() {
		return underlying.getSendCommandFeedback();
	}

	public void a(CommandObjectiveExecutor.EnumCommandResult result, int i) {
		underlying.a(result, i);
	}

	public void h() {
		underlying.h();
	}

	public void b(IChatBaseComponent ichatbasecomponent) {
		underlying.b(ichatbasecomponent);
	}

	public void a(boolean flag) {
		underlying.a(flag);
	}

	public boolean m() {
		return underlying.m();
	}

	public boolean a(EntityHuman entityhuman) {
		return override() || underlying.a(entityhuman);
	}

	public CommandObjectiveExecutor n() {
		return underlying.n();
	}

	public BlockPosition getChunkCoordinates() {
		return underlying.getChunkCoordinates();
	}

	public Vec3D d() {
		return underlying.d();
	}

	public World getWorld() {
		return underlying.getWorld();
	}

	public Entity f() {
		return underlying.f();
	}
}
