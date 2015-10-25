package ca.jarcode.consoles.computer.interpreter.luanative;

public interface LuaNInterface {
	long setupinst(int impl, long heap);
	long unrestrict(long ptr);
	int destroyinst(long ptr);
	void setdebug(int mode);
}
