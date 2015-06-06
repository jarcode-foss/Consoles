package jarcode.consoles.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

/*

This class represents metadata that can apply to any console

 */
public class ConsoleMeta {

	public Location location;
	public BlockFace face;
	public int w, h;

	public ConsoleMeta() {}
        
        public ConsoleMeta(byte[] arr) {
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(arr);
                DataInputStream din = new DataInputStream(in);
                double x = din.readDouble(), y = din.readDouble(), z = din.readDouble();
                float yaw = din.readFloat(), pitch = din.readFloat();
                String world = din.readUTF();
                location = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                face = BlockFace.valueOf(din.readUTF());
                w = din.readInt();
	            h = din.readInt();
            } catch (IOException ex) {
                throw new RuntimeException("Could not create console metadata from serialized data: ", ex);
            }
        }
	public ConsoleMeta(Location location, BlockFace face, int w, int h) {
		this.location = location;
		this.face = face;
		this.w = w;
		this.h = h;
	}
	public ManagedConsole spawnConsole() throws ConsoleCreateException {
		ManagedConsole console = new ManagedConsole(w, h);
		console.create(face, location);
		return console;
	}
	public ManagedConsole createConsole() {
		return new ManagedConsole(w, h);
	}

	public byte[] toBytes() {
		try {
			// serialize location
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dout = new DataOutputStream(out);
			double x = location.getX();
			double y = location.getY();
			double z = location.getZ();
			float yaw = location.getYaw();
			float pitch = location.getPitch();
			String world = location.getWorld().getName();
			dout.writeDouble(x);
			dout.writeDouble(y);
			dout.writeDouble(z);
			dout.writeFloat(yaw);
			dout.writeFloat(pitch);
			dout.writeUTF(world);
			// block face
			dout.writeUTF(face.name());
			// width and height
			dout.writeInt(w);
			dout.writeInt(h);
			return out.toByteArray();
		}
		// this shouldn't be thrown, we're writing to a stream whose underlying
		// type is ByteArrayOutputStream
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
