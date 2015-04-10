package jarcode.consoles;

import net.minecraft.server.v1_8_R2.*;

import java.lang.reflect.Field;
import java.util.Arrays;

public class CommandBlockListenerWrapper extends CommandBlockListenerAbstract {

	private static final Field COMMAND_RESULT, CHAT_COMPONENT;

	private static final String[] OVERRIDEN_COMMANDS = {"link"};

	private ConsoleListener consoleListener;

	static {
		try {
			COMMAND_RESULT = CommandBlockListenerAbstract.class.getDeclaredField("b");
			CHAT_COMPONENT = CommandBlockListenerAbstract.class.getDeclaredField("d");
			COMMAND_RESULT.setAccessible(true);
			CHAT_COMPONENT.setAccessible(true);
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

	@Override
	public int j() {
		return underlying.j();
	}

	@Override
	public IChatBaseComponent k() {
		return underlying.k();
	}

	@Override
	public void a(NBTTagCompound nbttagcompound) {
		underlying.a(nbttagcompound);
	}

	@Override
	public void b(NBTTagCompound nbttagcompound) {
		underlying.b(nbttagcompound);
	}

	@Override
	public boolean a(int i, String s) {
		return underlying.a(i, s);
	}

	@Override
	public void setCommand(String s) {
		underlying.setCommand(s);
	}

	@Override
	public String getCommand() {
		return underlying.getCommand();
	}
	private boolean override() {
		String command = getCommand().toLowerCase();
		if(command.startsWith("/")) {
			command = command.substring(1);
		}
		command = command.split(" ")[0];
		return Arrays.asList(OVERRIDEN_COMMANDS).contains(command);
	}
	@Override
	public void a(World world) {
		if (consoleListener != null) {
			sendMessage(new ChatComponentText(consoleListener.execute(this.sender, getCommand())));
			setResult(0);
			return;
		}
		if (!(ConsoleHandler.getInstance().commandBlocksEnabled || override())) {
			setResult(0);
			sendMessage(new ChatComponentText("You cannot use server commands"));
			return;
		}
		if(world.isClientSide) {
			setResult(0);
		}
		MinecraftServer minecraftserver = MinecraftServer.getServer();
		if(minecraftserver != null && minecraftserver.N()) {
			minecraftserver.getCommandHandler();
			try {
				setChatComponent(null);
				setResult(executeCommand(this, this.sender, getCommand()));
			} catch (Throwable var6) {
				CrashReport crashreport = CrashReport.a(var6, "Executing command block");
				throw new ReportedException(crashreport);
			}
		} else {
			setResult(0);
		}
	}

	@Override
	public String getName() {
		return underlying.getName();
	}

	@Override
	public IChatBaseComponent getScoreboardDisplayName() {
		return underlying.getScoreboardDisplayName();
	}

	@Override
	public void setName(String s) {
		underlying.setName(s);
	}

	@Override
	public void sendMessage(IChatBaseComponent ichatbasecomponent) {
		underlying.sendMessage(ichatbasecomponent);
	}

	@Override
	public boolean getSendCommandFeedback() {
		return underlying.getSendCommandFeedback();
	}

	@Override
	public void a(CommandObjectiveExecutor.EnumCommandResult result, int i) {
		underlying.a(result, i);
	}

	@Override
	public void h() {
		underlying.h();
	}

	@Override
	public void b(IChatBaseComponent ichatbasecomponent) {
		underlying.b(ichatbasecomponent);
	}

	@Override
	public void a(boolean flag) {
		underlying.a(flag);
	}

	@Override
	public boolean m() {
		return underlying.m();
	}

	@Override
	public boolean a(EntityHuman entityhuman) {
		return override() || underlying.a(entityhuman);
	}

	@Override
	public CommandObjectiveExecutor n() {
		return underlying.n();
	}

	@Override
	public BlockPosition getChunkCoordinates() {
		return null;
	}

	@Override
	public Vec3D d() {
		return underlying.d();
	}

	@Override
	public World getWorld() {
		return underlying.getWorld();
	}

	@Override
	public Entity f() {
		return underlying.f();
	}
}
