package ca.jarcode.consoles.computer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ca.jarcode.consoles.internal.ConsoleCreateException;
import ca.jarcode.consoles.internal.ConsoleMeta;
import ca.jarcode.consoles.Consoles;
import ca.jarcode.consoles.computer.filesystem.SerializedFilesystem;
import ca.jarcode.consoles.util.LocalPosition;
import ca.jarcode.consoles.util.gson.LocalPositionTypeAdapter;
import ca.jarcode.consoles.util.gson.LocationTypeAdapter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

// this class represents a serialized computer
public class ComputerData {

	private static final Gson GSON;

	private static File computerFolder;


	// file structure:
	// - computers (folder)
	// |
	//  -- <hostname> (folder)
	//   |
	//    -- header.json (file - fields from this class)
	//    -- fs.dat (file - serialized filesystem)
	//    -- metadata.dat (file - serialized ConsoleMeta)
	static void init(){
		Plugin plugin = Consoles.getInstance();
		computerFolder = new File(plugin.getDataFolder().getAbsolutePath() +
				File.separator + "computers");
		if (!computerFolder.exists()) {
			File folder = plugin.getDataFolder();
			if (!folder.exists() && !folder.mkdir()) {
				plugin.getLogger().warning("Could not create plugin folder");
			}
			if (!computerFolder.mkdir()) {
				plugin.getLogger().warning("Could not create JSON scoreboard file");
			}
		}
	}

	static {
		GSON = new GsonBuilder()
				// we want readable json headers
				.setPrettyPrinting()
				.registerTypeAdapter(Location.class, new LocationTypeAdapter())
				.registerTypeAdapter(LocalPosition.class, new LocalPositionTypeAdapter())
				.serializeNulls()
				.create();
	}

	private transient Computer computer;

	private transient File filesystem;
	private transient ConsoleMeta meta;
	private transient String hostname;

	public UUID owner;
	public boolean built = true;

	// sets up a ComputerData object that is prepared to load from a folder
	public static ComputerData fromFolder(File folder, Function<ComputerData, Boolean> predicate) throws IOException {
		File header = new File(folder.getAbsolutePath() + File.separator + "header.json");
		File fs = new File(folder.getAbsolutePath() + File.separator + "fs.dat");
		File metadata = new File(folder.getAbsolutePath() + File.separator + "metadata.dat");
		validateFiles(header, fs, metadata);
		FileReader reader = new FileReader(header);
		ComputerData data = GSON.fromJson(reader, ComputerData.class);
		reader.close();
		if (predicate != null && !predicate.apply(data))
			return null;
		data.meta = readMetadata(new FileInputStream(metadata));
		data.filesystem = fs;
		data.hostname = folder.getName();
		return data;
	}

	// deletes the data that corresponds to the given computer's hostname
	public static boolean delete(String hostname) {
		File folder = new File(computerFolder.getAbsolutePath() + File.separator + hostname);
		if (!folder.exists())
			return false;
		try {
			FileUtils.deleteDirectory(folder);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	// renames a computer
	public static boolean rename(String old, String hostname) {
		File folder = new File(computerFolder.getAbsolutePath() + File.separator + old);
		return folder.exists() && folder.renameTo(new File(computerFolder.getAbsolutePath() + File.separator + hostname));
	}

	private static void makeFiles(File... files) throws IOException {
		for (File file : files) {
			if (!file.exists() && !file.createNewFile())
				throw new IOException("failed to create file: " + file.getAbsolutePath());
		}
	}

	private static void validateFiles(File... files) throws IOException {
		for (File file : files) {
			if (!file.exists())
				throw new FileNotFoundException(file.getAbsolutePath());
			if (file.isDirectory())
				throw new IOException(file.getAbsolutePath() + ": is a directory");
		}
	}

	// loads and instantiates all the computers currently saved to disk
	public static List<Computer> makeAll(Consumer<String> inactiveHandler) {
		Consoles.getInstance().getLogger().info("Loading computers...");
		File[] files = computerFolder.listFiles();
		List<Computer> list = new ArrayList<>();
		if (files != null) {
			int loaded = 0;
			for (File entry : files) {
				if (entry.isDirectory()) {
					try {
						ComputerData data = fromFolder(entry, d -> d.built);
						if (data != null) {
							list.add(data.toComputer(true));
							loaded++;
						}
						else {
							inactiveHandler.accept(entry.getName());
						}
					}
					catch (IOException e) {
						Consoles.getInstance().getLogger().severe("Failed to load computer:");
						e.printStackTrace();
					}
				}
			}
			Consoles.getInstance().getLogger().info("Loaded " + loaded + " computers.");
		}
		else Consoles.getInstance().getLogger().severe("Could not load saved computers, failed to obtain files");
		return list;
	}
	public static boolean updateHeader(String hostname, Consumer<ComputerData> transformer) {
		File target = new File(computerFolder.getAbsolutePath() + File.separatorChar + hostname);
		if (target.exists() && target.isDirectory()) {
			try {
				File header = new File(target.getAbsolutePath() + File.separator + "header.json");
				validateFiles(header);
				FileReader reader = new FileReader(header);
				ComputerData data = GSON.fromJson(reader, ComputerData.class);
				reader.close();
				transformer.accept(data);
				FileWriter writer = new FileWriter(header);
				writer.write(GSON.toJson(data, ComputerData.class));
				writer.close();
				return true;
			}
			catch (IOException e) {
				Consoles.getInstance().getLogger().severe("Failed to load computer:");
				e.printStackTrace();
			}
		}
		return false;
	}
	public static boolean updateMeta(String hostname, Consumer<ConsoleMeta> transformer) {
		File target = new File(computerFolder.getAbsolutePath() + File.separatorChar + hostname);
		if (target.exists() && target.isDirectory()) {
			try {
				File metadata = new File(target.getAbsolutePath() + File.separator + "metadata.dat");
				validateFiles(metadata);
				ConsoleMeta meta = readMetadata(new FileInputStream(metadata));
				transformer.accept(meta);
				writeMetadata(new FileOutputStream(metadata), meta);
				return true;
			}
			catch (IOException e) {
				Consoles.getInstance().getLogger().severe("Failed to load computer:");
				e.printStackTrace();
			}
		}
		return false;
	}
	public static ManagedComputer load(String hostname) {
		File target = new File(computerFolder.getAbsolutePath() + File.separatorChar + hostname);
		if (target.exists() && target.isDirectory()) {
			try {
				ComputerData data = fromFolder(target, null);
				if (data == null) return null;
				return data.toComputer(false);
			}
			catch (IOException e) {
				Consoles.getInstance().getLogger().severe("Failed to load computer:");
				e.printStackTrace();
			}
		}
		return null;
	}

	public ComputerData() {}
	public ComputerData(Computer computer) {
		this.computer = computer;
		hostname = computer.getHostname();
		owner = computer.getOwner();
		meta = computer.getConsole().createMeta();
	}
	// if this ComputerData object was created from a computer, this method is used to
	// save everything to file.
	public void save() throws IOException {
		File folder = new File(computerFolder.getAbsolutePath() + File.separator + hostname);
		if (!folder.exists() && !folder.mkdir())
			throw new IOException("failed to create folder: " + folder.getAbsolutePath());
		File header = new File(folder.getAbsolutePath() + File.separator + "header.json");
		File fs = new File(folder.getAbsolutePath() + File.separator + "fs.dat");
		File metadata = new File(folder.getAbsolutePath() + File.separator + "metadata.dat");
		makeFiles(header, fs, metadata);
		writeMetadata(new FileOutputStream(metadata), meta);
		SerializedFilesystem files = new SerializedFilesystem(computer);
		files.serialize();
		files.writeTo(new FileOutputStream(fs));
		FileWriter writer = new FileWriter(header);
		writer.write(GSON.toJson(this, ComputerData.class));
		writer.close();
	}
	// if this ComputerData object was created from a folder, this creates the computer
	public ManagedComputer toComputer(boolean create) throws IOException {
		ManagedComputer computer = new ManagedComputer(hostname, owner, meta.createConsole());
		computer.load(filesystem);
		if (create) try {
			computer.create(meta.face, meta.location);
		} catch (ConsoleCreateException e) {
			Consoles.getInstance().getLogger().severe("Failed to place computer (cancelled by external plugin)");
			if (Consoles.debug)
				e.printStackTrace();
		}
		return computer;
	}
	@SuppressWarnings("unused")
	private void writeBlankJson(OutputStream out) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(computerFolder);
			fos.write("{}".getBytes());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (fos != null) {
				try {
					fos.flush();
					fos.close();
				}
				catch (IOException ignored) {}
			}
		}
	}
	private static void writeMetadata(OutputStream out, ConsoleMeta meta) throws IOException {
		DataOutputStream data = new DataOutputStream(out);
		byte[] arr = meta.toBytes();
		data.writeInt(arr.length);
		data.write(arr);
		out.close();
	}
	private static ConsoleMeta readMetadata(InputStream in) throws IOException {
		DataInputStream data = new DataInputStream(in);
		ConsoleMeta meta;
		int len = data.readInt();
		byte[] bytes = new byte[len];
		if (data.read(bytes) != bytes.length)
			throw new IOException("did not fully read meta from header");
		meta = new ConsoleMeta(bytes);
		in.close();
		return meta;
	}
}
