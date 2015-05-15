package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntryInteger extends ConstantPoolEntry {
    protected final int value;

    public ConstantPoolEntryInteger(int tag, int value) {
        super(tag);
        this.value = value;
    }

    public int getValue( ) {
        return value;
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
        super.write(dout);
        dout.writeInt(value);
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public static class Factory extends ConstantPoolEntry.Factory {
        protected int value;

        @Override
        public void read(int tag, DataInputStream din) throws IOException {
            super.read(tag, din);
            value = din.readInt();
        }

        @Override
        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntryInteger(tag, value);
        }
    }
}
