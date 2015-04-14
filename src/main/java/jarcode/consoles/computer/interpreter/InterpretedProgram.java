package jarcode.consoles.computer.interpreter;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Program;
import jarcode.consoles.computer.ProgramInstance;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;

public class InterpretedProgram implements Program {

	private FSFile file;
	private InputStream in;
	private OutputStream out;
	private Computer computer;
	private Charset charset = Charset.forName("UTF-8");
	private HashMap<String, LibFunction> instanceTable = new HashMap<String, LibFunction>();
	private ProgramInstance instance;

	public InterpretedProgram(FSFile file) {
		this.file = file;
	}
	{
		instanceTable.put("print", Lua.link(this::print));
		instanceTable.put("println", Lua.link(this::println));
	}
	public void run(OutputStream out, InputStream in, String str, Computer computer, ProgramInstance instance) throws Exception {
		this.in = in;
		this.out = out;
		this.computer = computer;
		this.instance = instance;
		if (file == null) {
			print("null file");
			return;
		}
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		try (InputStream is = file.createInput()) {
			int i;
			while (true) {
				if (terminated())
					break;
				if (is.available() > 0 || is instanceof ByteArrayInputStream) {
					i = is.read();
					if (i == -1) break;
					buf.write(i);
				} else Thread.sleep(50);
			}
			if (terminated())
				print(" [PARSE TERMINATED]");
		}
		String raw = new String(buf.toByteArray(), charset);
		Globals globals = new Globals();
		globals.load(new JseBaseLib());
		globals.load(new PackageLib());
		globals.load(new Bit32Lib());
		globals.load(new TableLib());
		globals.load(new StringLib());
		globals.load(new JseMathLib());
		globals.load(new GameIoLib(instance));
		globals.load(new InterruptLib());
		globals.load(new MathLib());
		LoadState.install(globals);
		LuaC.install(globals);

		globals.load(new GameLib());
		globals.load(new BaseLib());
		globals.load(new InstanceLib(instanceTable));
		LuaValue chunk = globals.load(raw);
		chunk.call();
		/*
		LuaValue v = globals.get("main");
		LuaValue ret = v.call(LuaValue.valueOf(str));
		if (ret.isint()) {
			print("Exited with code: " + ret.checkint());
		}
		else
			print("Exited with code: -1");
			*/
	}
	protected void print(String formatted) {
		try {
			out.write(formatted.getBytes(charset));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected void println(String formatted) {
		print(formatted + '\n');
	}
	protected void nextln() {
		print("\n");
	}
	protected FSBlock resolve(String input) {
		return computer.resolve(input, this);
	}
	protected boolean terminated() {
		return instance.isTerminated();
	}
}
