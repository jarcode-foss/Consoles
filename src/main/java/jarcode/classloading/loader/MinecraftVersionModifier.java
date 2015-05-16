package jarcode.classloading.loader;

import jarcode.classloading.instrument.*;
import org.bukkit.plugin.Plugin;

import java.io.*;

public class MinecraftVersionModifier implements ClassModifier {

	private static final String PREFIX_CB = "org/bukkit/craftbukkit/";
	private static final String PREFIX_NMS = "net/minecraft/server/";
	// the fake prefix to use for class mappings
	private String pkg;
	private String current;

	public MinecraftVersionModifier(Plugin plugin, String current) {
		pkg = plugin.getServer().getClass().getPackage().getName();
		pkg = pkg.substring(pkg.lastIndexOf('.') + 1);
		this.current = current;
	}

	// credit to ikillforeyou/Overcaste for this code
	@Override
	public byte[] instrument(byte[] in, String classname) {
		ByteArrayOutputStream ret = new ByteArrayOutputStream();
		ByteArrayInputStream src = new ByteArrayInputStream(in);

		System.out.println("\nInstrumenting: " + classname + "\n");

		try (DataInputStream din = new DataInputStream(src); DataOutputStream dout = new DataOutputStream(ret)) {
			dout.writeInt(din.readInt()); // Ignore magic.
			dout.writeShort(din.readUnsignedShort()); // Ignore minor
			dout.writeShort(din.readUnsignedShort()); // Ignore major
			int constantPoolCount = din.readUnsignedShort();
			ConstantPoolEntry[] constantPool = new ConstantPoolEntry[constantPoolCount];
			for (int i = 1; i < constantPoolCount; i++) {
				constantPool[i] = ConstantPoolEntries.readEntry(din);
				if (constantPool[i] == null) {
					System.out.println("Invalid constant pool for index: " + constantPoolCount);
				}
				if(constantPool[i] instanceof ConstantPoolEntryLong || constantPool[i] instanceof ConstantPoolEntryDouble) {
					i++;
				}
			}
			dout.writeShort(constantPoolCount);
			for (ConstantPoolEntry e : constantPool) {
				if (e != null) {
					ConstantPoolEntry written = e;
					if (e instanceof ConstantPoolEntryUtf8) {
						// Replace all instances of the explicit version to our value.
						written = new ConstantPoolEntryUtf8(e.getTag(),
								((ConstantPoolEntryUtf8) e).getValue()
										.replace(PREFIX_CB + current, PREFIX_CB + pkg)
										.replace(PREFIX_NMS + current, PREFIX_NMS + pkg));
					}
					written.write(dout);
				}
			}

			byte[] buffer = new byte[2048]; // We only care about the constant pool, everything else can be written.
			int bytesRead;
			while ((bytesRead = din.read(buffer)) != -1) {
				dout.write(buffer, 0, bytesRead);
			}
			return ret.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return in;
		}
	}
}
