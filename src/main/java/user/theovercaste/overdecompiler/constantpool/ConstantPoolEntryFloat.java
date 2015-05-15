package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntryFloat extends ConstantPoolEntry {
    protected final float value;

    public ConstantPoolEntryFloat(int tag, float value) {
        super(tag);
        this.value = value;
    }

    public float getValue( ) {
        return value;
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
        super.write(dout);
        dout.writeFloat(value);
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public static class Factory extends ConstantPoolEntry.Factory {
        protected float value;

        @Override
        public void read(int tag, DataInputStream din) throws IOException {
            super.read(tag, din);
            value = din.readFloat();
        }

        @Override
        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntryFloat(tag, value);
        }
    }
}
