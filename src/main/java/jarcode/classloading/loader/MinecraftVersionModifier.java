package jarcode.classloading.loader;

import user.theovercaste.overdecompiler.constantpool.ConstantPoolEntries;
import user.theovercaste.overdecompiler.constantpool.ConstantPoolEntry;
import user.theovercaste.overdecompiler.constantpool.ConstantPoolEntryUtf8;

import java.io.*;

public class MinecraftVersionModifier implements ClassModifier {

	private static final String PREFIX = "net/minecraft/server/v1_8_R2/";
	private static final String REPLACED = "net/minecraft/server/version/";

	// credit to ikillforeyou/Overcaste for this code
	@Override
	public byte[] instrument(byte[] in, String classname) {
		ByteArrayOutputStream ret = new ByteArrayOutputStream();
		ByteArrayInputStream src = new ByteArrayInputStream(in);

		try (DataInputStream din = new DataInputStream(src); DataOutputStream dout = new DataOutputStream(ret)) {
			dout.writeInt(din.readInt()); // Ignore magic.
			dout.writeShort(din.readUnsignedShort()); // Ignore minor
			dout.writeShort(din.readUnsignedShort()); // Ignore major
			int constantPoolCount = din.readUnsignedShort();
			ConstantPoolEntry[] constantPool = new ConstantPoolEntry[constantPoolCount];
			for (int i = 1; i < constantPoolCount; i++) {
				constantPool[i] = ConstantPoolEntries.readEntry(din);
				if (constantPool[i] == null) {
					System.out.println("Invalid constant pool entry found: " + i);
					constantPoolCount--;
				}
			}
			dout.writeShort(constantPoolCount);
			for (ConstantPoolEntry e : constantPool) {
				if (e != null) {
					ConstantPoolEntry written = e;
					if (e.getTag() == ConstantPoolEntries.UTF8_TAG) {
						// Replace all instances of the explicit version to our value.
						written = new ConstantPoolEntryUtf8(e.getTag(),
								((ConstantPoolEntryUtf8) e).getValue().replace(PREFIX, REPLACED));
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
