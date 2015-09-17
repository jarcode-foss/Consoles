package ca.jarcode.classloading.loader;

import ca.jarcode.classloading.instrument.*;
import org.bukkit.plugin.Plugin;

import java.io.*;

/*

This is a class modifier that instruments our class data directly, replacing constants in
the constant pool of the class's bytecode to allow for package ambiguity in this code.

the UTF-8 values for the import statements in java bytecode is stored in the constant
pool as a UTF-8 entry, so we replace that data.

Thanks to ikillforeyou who suggested instrumentation as a solution for version
compatibility

 */
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

	// credit to ikillforeyou/Overcaste for most of this code
	@Override
	public byte[] instrument(byte[] in, String classname) {
		ByteArrayOutputStream ret = new ByteArrayOutputStream();
		ByteArrayInputStream src = new ByteArrayInputStream(in);

		try (DataInputStream din = new DataInputStream(src); DataOutputStream dout = new DataOutputStream(ret)) {

			// this is the header for a class file, we skip this data

			dout.writeInt(din.readInt()); // Ignore magic.
			dout.writeShort(din.readUnsignedShort()); // Ignore minor
			dout.writeShort(din.readUnsignedShort()); // Ignore major

			// the amount of pool entries, minus one
			int constantPoolCount = din.readUnsignedShort();

			ConstantPoolEntry[] constantPool = new ConstantPoolEntry[constantPoolCount];

			// iterate through our entries
			for (int i = 1; i < constantPoolCount; i++) {
				// decode the entry
				constantPool[i] = ConstantPoolEntries.readEntry(din);
				// something went wrong if we didn't get an entry back!
				if (constantPool[i] == null) {
					System.out.println("Invalid constant pool for index: " + constantPoolCount);
				}
				// this is an edge case - is this is a long or double entry, it increments the index twice.
				// I have no idea why.
				if(constantPool[i] instanceof ConstantPoolEntryLong || constantPool[i] instanceof ConstantPoolEntryDouble) {
					i++;
				}
			}

			// write our pool count back
			dout.writeShort(constantPoolCount);
			for (ConstantPoolEntry e : constantPool) {
				// write pool entries
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
