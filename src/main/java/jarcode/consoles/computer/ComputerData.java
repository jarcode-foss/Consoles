package jarcode.consoles.computer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jarcode.consoles.ConsoleMeta;
import jarcode.consoles.Consoles;
import jarcode.consoles.computer.filesystem.SerializedFilesystem;
import jarcode.consoles.util.LocalPosition;
import jarcode.consoles.util.gson.LocalPositionTypeAdapter;
import jarcode.consoles.util.gson.LocationTypeAdapter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// this class represents a serialized computer
public class ComputerData {

	private static final Gson GSON;

	private static File file;


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
		file = new File(plugin.getDataFolder().getAbsolutePath() +
				File.separator + "computers");
		if (!file.exists()) {
			File folder = plugin.getDataFolder();
			if (!folder.exists() && !folder.mkdir()) {
				plugin.getLogger().warning("Could not create plugin folder");
			}
			if (!file.mkdir()) {
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

	private UUID owner;

	// sets up a ComputerData object that is prepared to load from a folder
	public static ComputerData fromFolder(File folder) throws IOException {
		File header = new File(folder.getAbsolutePath() + File.separator + "header.json");
		File fs = new File(folder.getAbsolutePath() + File.separator + "fs.dat");
		File metadata = new File(folder.getAbsolutePath() + File.separator + "metadata.dat");
		validateFiles(header, fs, metadata);
		ComputerData data = GSON.fromJson(new FileReader(header), ComputerData.class);
		data.readMetadata(new FileInputStream(metadata));
		data.filesystem = fs;
		data.hostname = folder.getName();
		return data;
	}

	// deletes the data that corresponds to the given computer's hostname
	public static boolean delete(String hostname) {
		File folder = new File(file.getAbsolutePath() + File.separator + hostname);
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
		File folder = new File(file.getAbsolutePath() + File.separator + old);
		return folder.exists() && folder.renameTo(new File(file.getAbsolutePath() + File.separator + hostname));
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
	public static List<Computer> makeAll() {
		Consoles.getInstance().getLogger().info("Loading computers...");
		File[] files = file.listFiles();
		List<Computer> list = new ArrayList<>();
		if (files != null) {
			int loaded = 0;
			for (File entry : files) {
				if (entry.isDirectory()) {
					try {
						ComputerData data = fromFolder(entry);
						list.add(data.toComputer());
						loaded++;
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
		File folder = new File(file.getAbsolutePath() + File.separator + hostname);
		if (!folder.exists() && !folder.mkdir())
			throw new IOException("failed to create folder: " + folder.getAbsolutePath());
		File header = new File(folder.getAbsolutePath() + File.separator + "header.json");
		File fs = new File(folder.getAbsolutePath() + File.separator + "fs.dat");
		File metadata = new File(folder.getAbsolutePath() + File.separator + "metadata.dat");
		makeFiles(header, fs, metadata);
		writeMetadata(new FileOutputStream(metadata));
		SerializedFilesystem files = new SerializedFilesystem(computer);
		files.serialize();
		files.writeTo(new FileOutputStream(fs));
		FileWriter writer = new FileWriter(header);
		writer.write(GSON.toJson(this, ComputerData.class));
		writer.close();
	}
	// if this ComputerData object was created from a folder, this creates the computer
	public Computer toComputer() throws IOException {
		ManagedComputer computer = new ManagedComputer(hostname, owner, meta.createConsole());
		computer.load(filesystem);
		computer.create(meta.face, meta.location);
		return computer;
	}
	private void writeMetadata(OutputStream out) throws IOException {
		DataOutputStream data = new DataOutputStream(out);
		byte[] arr = meta.toBytes();
		if (Consoles.DEBUG) {
			Consoles.getInstance().getLogger().info("[DEBUG] Writing " + arr.length + " byte metadata file...");
			Consoles.getInstance().getLogger().info("[DEBUG] str: " + new String(arr));
		}
		data.writeInt(arr.length);
		data.write(arr);
		out.close();
	}
	@SuppressWarnings("unused")
	private void writeBlankJson(OutputStream out) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
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
	private void readMetadata(InputStream in) throws IOException {
		DataInputStream data = new DataInputStream(in);
		int len = data.readInt();
		byte[] bytes = new byte[len];
		if (data.read(bytes) != bytes.length)
			throw new IOException("did not fully read meta from header");
		meta = new ConsoleMeta(bytes);
		in.close();
	}
}
