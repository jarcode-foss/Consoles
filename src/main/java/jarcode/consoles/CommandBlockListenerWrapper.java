package jarcode.consoles;

import net.minecraft.server.v1_8_R3.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class CommandBlockListenerWrapper extends CommandBlockListenerAbstract {

	private static final Field COMMAND_RESULT, CHAT_COMPONENT;

	private static final String[] OVERRIDEN_COMMANDS = {"link"};

	private ConsoleListener consoleListener;

	static {
		try {
			// these field names are sensitive, they have changed recently
			if (Pkg.is("v1_8_R2")) {
				COMMAND_RESULT = CommandBlockListenerAbstract.class.getDeclaredField("b");
				CHAT_COMPONENT = CommandBlockListenerAbstract.class.getDeclaredField("d");
				COMMAND_RESULT.setAccessible(true);
				CHAT_COMPONENT.setAccessible(true);
			}
			else throw new RuntimeException("Unsupported server version: " + Pkg.VERSION);
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

	CommandBlockListenerAbstract underlying;
	// this is a cheat to make this act like an inner class when we get this$0 via reflection later
	@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
	private final TileEntityCommand this$0;

	CommandBlockListenerWrapper(CommandBlockListenerAbstract underlying, TileEntityCommand command) {
		this.underlying = underlying;
		this.this$0 = command;
	}

	public void setConsoleListener(ConsoleListener listener) {
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
		return Arrays.asList(OVERRIDEN_COMMANDS).contains(command);
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
			sendMessage(new ChatComponentText(consoleListener.execute(this.sender, getCommand())));
			setResult(0);
			return;
		}
		if (!ConsoleHandler.getInstance().commandBlocksEnabled && !override()) {
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
				setResult(executeCommand(this, this.sender, getCommand()));
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
		String name;
		switch (Pkg.VERSION) {
			case "v1_8_R2":
				name = "N";
				break;
			case "v1_8_R3":
				name = "O";
				break;
			default: throw new RuntimeException("Unsupported server version: " + Pkg.VERSION);
		}
		try {
			Method method = MinecraftServer.class.getMethod(name);
			return (Boolean) method.invoke(this);
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
		return null;
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
