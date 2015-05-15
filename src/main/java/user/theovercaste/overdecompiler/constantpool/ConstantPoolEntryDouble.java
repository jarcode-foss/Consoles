package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntryDouble extends ConstantPoolEntry {
    protected final double value;

    public ConstantPoolEntryDouble(int tag, double value) {
        super(tag);
        this.value = value;
    }

    public double getValue( ) {
        return value;
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
        super.write(dout);
        dout.writeDouble(value);
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public static class Factory extends ConstantPoolEntry.Factory {
        protected double value;

        @Override
        public void read(int tag, DataInputStream din) throws IOException {
            super.read(tag, din);
            value = din.readDouble();
        }

        @Override
        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntryDouble(tag, value);
        }
    }
}
