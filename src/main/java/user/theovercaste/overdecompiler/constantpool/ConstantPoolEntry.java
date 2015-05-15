package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntry {
    protected final int tag;

    public ConstantPoolEntry(int tag) {
        this.tag = tag;
    }

    public int getTag( ) {
        return tag;
    }

    public void write(DataOutputStream dout) throws IOException {
        dout.writeByte(tag);
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public static class Factory {
        protected int tag;

        public void read(int tag, DataInputStream din) throws IOException {
            this.tag = tag;
        }

        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntry(tag);
        }
    }
}
