package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.*;
import jarcode.consoles.computer.manual.ProvidedManual;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static jarcode.consoles.computer.ProgramUtils.*;


@ProvidedManual(
		author = "Jarcode",
		version = "1.6",
		contents = "Copies a file or folder from one path location to another."
)
public class CopyProgram extends FSProvidedProgram {
	@Override
	public void run(String in, Computer computer) throws Exception {

		String[] args = splitArguments(in);
		if (args.length < 2 || in.trim().isEmpty()) {
			printUsage();
			return;
		}
		Terminal terminal = computer.getTerminal(this);
		FSBlock sourceBlock = computer.getBlock(args[0], terminal.getCurrentDirectory());
		FSBlock targetBlock = computer.getBlock(args[1], terminal.getCurrentDirectory());
		if (sourceBlock == null) {
			print("cp: " + args[0].trim() + ": file does not exist");
			return;
		}
		if (targetBlock != null) {
			print("cp: " + args[1].trim() + ": file exists");
			return;
		}
		String[] arr = FSBlock.section(args[0], "/");
		String sourceBase = Arrays.asList(arr).stream()
				.limit(arr.length == 0 ? 0 : arr.length - 1)
				.collect(Collectors.joining("/"));
		if (sourceBase.trim().isEmpty() && args[0].startsWith("/"))
			sourceBase = "/";
		String sourceFile = Arrays.asList(arr).stream()
				.filter(s -> !s.isEmpty())
				.reduce((o1, o2) -> o2)
				.get();
		arr = FSBlock.section(args[1], "/");
		String targetBase = Arrays.asList(arr).stream()
				.limit(arr.length == 0 ? 0 : arr.length - 1)
				.collect(Collectors.joining("/"));
		if (targetBase.trim().isEmpty() && args[1].startsWith("/"))
			targetBase = "/";
		String targetFile = Arrays.asList(arr).stream()
				.filter(s -> !s.isEmpty())
				.reduce((o1, o2) -> o2)
				.get();
		if (!FSBlock.allowedBlockName(sourceFile)) {
			print("cp: " + sourceFile.trim() + ": bad block name");
			return;
		}
		if (!FSBlock.allowedBlockName(targetFile)) {
			print("cp: " + targetFile.trim() + ": bad block name");
			return;
		}
		FSBlock sourceBaseFolder = computer.getBlock(sourceBase, terminal.getCurrentDirectory());
		if (sourceBaseFolder == null) {
			print("cp: " + sourceBase.trim() + ": does not exist");
			return;
		}
		FSBlock targetBaseFolder = computer.getBlock(targetBase, terminal.getCurrentDirectory());
		if (targetBaseFolder == null) {
			print("cp: " + targetBase.trim() + ": does not exist");
			return;
		}
		if (!(targetBaseFolder instanceof FSFolder)) {
			print("cp: " + targetBase + ": not a folder");
			return;
		}
		FSFolder b = ((FSFolder) sourceBaseFolder);
		FSFolder k = ((FSFolder) targetBaseFolder);
		FSBlock a = b.contents.get(sourceFile);
		k.contents.put(targetFile, copy(a));
	}
	private FSStoredFile fileCopy(FSStoredFile file) throws IOException, InterruptedException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Charset charset = Charset.forName("UTF-8");
		try (InputStream is = file.createInput()) {
			int i;
			while (true) {
				if (terminated())
					break;
				if (is.available() > 0 || is instanceof ByteArrayInputStream) {
					i = is.read();
					if (i == -1) break;
					out.write((byte) i);
				} else {
					Thread.sleep(50);
				}
			}
			if (terminated())
				print("\tterminated");
		}
		return writtenFile(new String(out.toByteArray(), charset));

	}
	private FSStoredFile writtenFile(String text) throws IOException {
		FSStoredFile file = new FSStoredFile();
		OutputStream out = file.createOutput();
		out.write(text.getBytes(Charset.forName("UTF-8")));
		out.close();
		return file;
	}
	private FSBlock copy(FSBlock blk) throws IOException, InterruptedException {
		if (blk instanceof FSStoredFile) {
			return fileCopy((FSStoredFile) blk);
		}
		else if (blk instanceof FSFolder) {
			FSFolder a = (FSFolder) blk;
			FSFolder folder = new FSFolder();
			for (Map.Entry<String, FSBlock> entry : a.contents.entrySet()) {
				if (terminated())
					break;
				FSBlock block = copy(entry.getValue());
				if (block != null)
					folder.contents.put(entry.getKey(), block);
			}
			return folder;
		}
		else if (blk instanceof FSProvidedProgram) {
			return blk;
		}
		else return null;
	}
	private void printUsage() {
		println("Usage: cp [SOURCE] [TARGET] ");
	}
}
