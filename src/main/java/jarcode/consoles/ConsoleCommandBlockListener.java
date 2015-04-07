package jarcode.consoles;

import net.minecraft.server.v1_8_R1.*;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonObject;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonPrimitive;
import org.bukkit.craftbukkit.v1_8_R1.command.CraftBlockCommandSender;

import java.lang.reflect.Field;

public class ConsoleCommandBlockListener extends CommandBlockListenerAbstract {
	TileEntityCommand tileEntity;
	private ConsoleListener consoleListener;
	ConsoleCommandBlockListener(TileEntityCommandListener listener, ConsoleListener consoleListener) {
		this.consoleListener = consoleListener;
		try {
			Field field = TileEntityCommandListener.class.getDeclaredField("a");
			field.setAccessible(true);
			this.tileEntity = (TileEntityCommand) field.get(listener);
			this.sender = new CraftBlockCommandSender(listener);
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalArgumentException("Invalid listener", e);
		}
	}

	public BlockPosition getChunkCoordinates() {
		return this.tileEntity.getPosition();
	}

	public int execute(CommandSender bSender, String command) {
		String result = consoleListener.execute(bSender, command);
		setOutput(result);
		return 0;
	}

	public void setOutput(String str) {

		JsonObject object = new JsonObject();
		object.add("text", new JsonPrimitive(str));
		object.add("color", new JsonPrimitive("aqua"));

		IChatBaseComponent component = ChatSerializer.a(object.toString());
		try {
			Field field = CommandBlockListenerAbstract.class.getDeclaredField("d");
			field.setAccessible(true);
			field.set(this, component);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public Vec3D d() {
		return new Vec3D((double)this.tileEntity.getPosition().getX() + 0.5D,
				(double)this.tileEntity.getPosition().getY() + 0.5D, (double)this.tileEntity.getPosition().getZ() + 0.5D);
	}
	public void a(World world) {
		execute(this.sender, this.e);
	}
	public World getWorld() {
		return this.tileEntity.getWorld();
	}

	public void setCommand(String s) {
		super.setCommand(s);
		this.tileEntity.update();
	}

	public void h() {
		this.tileEntity.getWorld().notify(this.tileEntity.getPosition());
	}

	public Entity f() {
		return null;
	}
}
