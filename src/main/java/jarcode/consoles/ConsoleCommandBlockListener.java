package jarcode.consoles;

import com.google.gson.JsonPrimitive;
import net.minecraft.server.v1_8_R2.*;
import org.bukkit.command.CommandSender;
import com.google.gson.JsonObject;
import org.bukkit.craftbukkit.v1_8_R2.command.CraftBlockCommandSender;

import java.lang.reflect.Field;

public class ConsoleCommandBlockListener extends CommandBlockListenerAbstract {

	TileEntityCommand tileEntity;
	private ConsoleListener consoleListener;

	ConsoleCommandBlockListener(CommandBlockListenerAbstract listener, ConsoleListener consoleListener,
	                            TileEntityCommand tileEntity) {

		this.consoleListener = consoleListener;
		this.tileEntity = tileEntity;
		this.sender = new CraftBlockCommandSender(listener);
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

		IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(object.toString());
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
		execute(this.sender, this.getCommand());
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
