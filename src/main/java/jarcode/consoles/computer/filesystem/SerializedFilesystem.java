package jarcode.consoles.computer.filesystem;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

// compact serialization for the filesystem. Not using java's built-in serialization because it's bloated,
// and JSON/GSON is another degree worse for serialized size...
public class SerializedFilesystem {

	private Computer computer;

	// Okay, here's the pudding of how this works. Because our filesystem is reference based, storing everything in a
	// tree won't cut it, and will produce duplicate file instances, but it still needs to be deserialized into a tree!

	// Because we identify files with UUIDs, we load a massive list of file serialized block instances, and when they
	// serialize, they can grab instances/trigger loads from other blocks/UUIDs. Sort of like how Java loads classes!
	private HashMap<UUID, FSBlock> mappings = new HashMap<>();
	private HashMap<UUID, byte[]> serializedMappings = new HashMap<>();

	private UUID root;

	public SerializedFilesystem(Computer computer) {
		this.computer = computer;
	}

	private HashMap<Byte, BlockSerializer<?>> map = new HashMap<>();
	{
		// We register everything we need to serialize/store in our little fake EXT filesystem

		// Notice devices don't get stored! This is the same in linux, they technically have no data, and represent
		// IO for real devices hooked up to the machine, so these are created on startup.

		register((byte) 0x01, new BlockSerializer<FSStoredFile>() {
			@Override
			public byte[] serialize(FSStoredFile type) {
				return ((ByteArrayOutputStream) type.getOutput()).toByteArray();
			}

			@Override
			public FSStoredFile deserialize(byte[] data, UUID uuid) {
				return new FSStoredFile(data, uuid);
			}
		});
		// this is a bit of an exception to our serialization process. We went users to be able to screw up their
		// provided programs in their filesystem, but provided programs don't have any data! They're just Java
		// functionality, so instead we use a separate ID system for all these provided programs (including the kernel)

		// Notice these are loaded from the kernel itself! That means the computer has to be running properly to
		// serialize :)
		register((byte) 0x02, new BlockSerializer<FSProvidedProgram>() {
			@Override
			public byte[] serialize(FSProvidedProgram type) {
				return new byte[] {computer.getKernel().getId(type)};
			}

			@Override
			public FSProvidedProgram deserialize(byte[] data, UUID uuid) {
				return computer.getKernel().getProgram(data[0]);
			}
		});
		register((byte) 0x00, new BlockSerializer<FSFolder>() {
			@Override
			public byte[] serialize(FSFolder type) {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				DataOutputStream out = new DataOutputStream(buffer);
				try {
					for (Map.Entry<String, FSBlock> entry : type.contents.entrySet()) {
						if (map.containsKey(entry.getValue().id)) {
							out.writeInt(entry.getKey().length());
							out.write(entry.getKey().getBytes(Charset.forName("UTF-8")));
							out.writeLong(entry.getValue().uuid.getMostSignificantBits());
							out.writeLong(entry.getValue().uuid.getLeastSignificantBits());
							if (Consoles.DEBUG) {
								Consoles.getInstance().getLogger().info("[DEBUG] FS: block entry: " + entry.getKey());
							}
						}
					}
				}
				// won't happen
				catch (IOException e) {
					e.printStackTrace();
				}
				return buffer.toByteArray();
			}

			@Override
			public FSFolder deserialize(byte[] data, UUID uuid) {
				ByteArrayInputStream buffer = new ByteArrayInputStream(data);
				DataInputStream in = new DataInputStream(buffer);
				FSFolder folder = new FSFolder();
				try {
					while (buffer.available() > 0) {
						int len = in.readInt(); // key length
						byte[] arr = new byte[len];
						for (int t = 0; t < len; t++) {
							arr[t] = (byte) in.read();
						}
						String key = new String(arr, Charset.forName("UTF-8")); // key
						long most = in.readLong();
						long least = in.readLong();
						UUID blockId = new UUID(most, least); // the UUID of the block this key is assigned to

						// now, we grab the block for the UUID of this key! This means everything will be recursively
						// deserialized if it doesn't already exist.
						FSBlock block = SerializedFilesystem.this.deserialize(blockId);
						folder.contents.put(key, block);
					}
				}
				// won't happen
				catch (IOException e) {
					e.printStackTrace();
				}
				return folder;
			}
		});
	}
	private <T extends FSBlock> void register(byte b, BlockSerializer<T> serializer) {
		map.put(b, serializer);
	}
	private interface BlockSerializer <T extends FSBlock> {
		public byte[] serialize(T type);
		public T deserialize(byte[] data, UUID uuid);
	}
	private boolean serializerExists(byte id) {
		return map.containsKey(id);
	}
	// Every block in the EXT filesystem have an owner and permissions. So, we have a header with exactly that!
	private byte[] getHeader(FSBlock block) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(out);
		try {
			data.writeInt(block.owner.length()); // string length
			data.write(block.owner.getBytes(Charset.forName("UTF-8"))); // owner
			data.write(block.permissions); // block permissions
			data.write(block.id); // block type (file/folder/provided/device)
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}
	@SuppressWarnings("unchecked")
	private <T extends FSBlock> byte[] toBytes(T block) {
		byte[] header = getHeader(block);
		BlockSerializer<T> serializer = (BlockSerializer<T>) map.get(block.id);
		byte[] content = serializer.serialize(block);
		byte[] data = new byte[header.length + content.length];
		System.arraycopy(header, 0, data, 0, header.length);
		System.arraycopy(content, 0, data, header.length, content.length);
		if (Consoles.DEBUG) {
			Consoles.getInstance().getLogger().info("[DEBUG] FS: writing block uuid: " + block.uuid + ", len: " + data.length);
		}
		return data;
	}
	public void serialize() {
		serialize(computer.getRoot(), true);
	}
	public void serialize(FSBlock root) {
		serialize(root, true);
	}
	// de-serialization is recursive, but serialization isn't! Folder serialization doesn't trigger anything, it just
	// stores the UUIDs of its contents.

	// This means we have to make this recursive :)
	private void serialize(FSBlock root, boolean first) {

		if (first)
			this.root = root.uuid;

		// put this in the mappings, if it doesn't already exist, and serialize it.
		if (!serializedMappings.containsKey(root.uuid))
			serializedMappings.put(root.uuid, toBytes(root));

		// if this is a folder, do the same for its entries.
		if (root instanceof FSFolder) {
			FSFolder folder = (FSFolder) root;
			folder.contents.values().stream()
					.filter(block -> map.containsKey(block.id)) // filter out device file types, etc
					.filter(block -> !serializedMappings.containsKey(block.uuid)) // only store one of each file
					.forEach(block -> {
						serializedMappings.put(block.uuid, toBytes(block)); // map the file to its uuid
						serialize(block, false); // serialize recursively
					});
		}
	}
	public FSBlock deserialize() throws IOException {
		return deserialize(root);
	}
	// Now, this is both our method for internal, recursive de-serialization, AND for triggering the de-serialization
	// of the entire filesystem! There's a problem here, because we have no way of getting the root folder from our
	// serialized mappings, so we DO need to store the root UUID so we know where to start when de-serializing our
	// tree.
	public FSBlock deserialize(UUID uuid) throws IOException {

		// if this file was already deserialized somewhere else, and this is a reference to the same file, use it!
		if (mappings.containsKey(uuid))
			return mappings.get(uuid);

		byte[] bytes = serializedMappings.get(uuid);
		if (bytes == null) {
			throw new IOException("Missing block: " + uuid);
		}
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		DataInputStream data = new DataInputStream(in);
		try {
			// header
			int len = data.readInt();
			byte[] arr = new byte[len];
			for (int t = 0; t < len; t++) {
				arr[t] = (byte) data.read();
			}
			String owner = new String(arr, Charset.forName("UTF-8"));
			byte permissions = (byte) data.read();
			byte id = (byte) data.read();
			// content
			byte[] remaining = new byte[in.available()];
			if (remaining.length != 0 && data.read(remaining) != remaining.length)
				throw new IOException(String.format(
						"in.available() did not properly represent remaining bytes (id:%d,len:%d)",
						id, remaining.length
				));
			BlockSerializer serializer = map.get(id);
			FSBlock block = serializer.deserialize(remaining, uuid);
			block.permissions = permissions;
			block.owner = owner;
			return block;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	// write this entire filesystem somewhere! serialize(root) should be called first.
	public void writeTo(OutputStream out) throws IOException {
		DataOutputStream data = null;
		GZIPOutputStream wrapped = null;
		try {
			wrapped = new GZIPOutputStream(out, 512, true);
			data = new DataOutputStream(wrapped);
			data.writeLong(root.getMostSignificantBits());
			data.writeLong(root.getLeastSignificantBits());
			for (Map.Entry<UUID, byte[]> entry : serializedMappings.entrySet()) {
				data.writeBoolean(true);
				data.writeLong(entry.getKey().getMostSignificantBits());
				data.writeLong(entry.getKey().getLeastSignificantBits());
				data.writeInt(entry.getValue().length);
				data.write(entry.getValue());
			}
			data.writeBoolean(false);
		}
		finally {
			if (wrapped != null) {
				wrapped.finish();
				wrapped.flush();
			}
			if (data != null) {
				data.flush();
				data.close();
			}
		}
	}
	public void readFrom(InputStream in) throws IOException {
		DataInputStream data = null;
		GZIPInputStream wrapped = null;
		try {
			wrapped = new GZIPInputStream(in, 512);
			data = new DataInputStream(wrapped);
			long most = data.readLong();
			long least = data.readLong();
			root = new UUID(most, least);
			while (data.readBoolean()) {
				most = data.readLong();
				least = data.readLong();
				UUID uuid = new UUID(most, least);
				int len = data.readInt();
				byte[] bytes = new byte[len];
				if (data.read(bytes) != len)
					throw new IOException("failed to read block: invalid length");
				serializedMappings.put(uuid, bytes);
				if (Consoles.DEBUG)
					Consoles.getInstance().getLogger().info("read block: " + uuid + ", len: " + len);
			}
		}
		finally {
			if (wrapped != null)
				wrapped.close();
			if (data != null)
				data.close();
		}
	}
}
