package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntryMethodType extends ConstantPoolEntry {
    protected final int descriptorIndex;

    public ConstantPoolEntryMethodType(int tag, int descriptorIndex) {
        super(tag);
        this.descriptorIndex = descriptorIndex;
    }

    public int getDescriptorIndex( ) {
        return descriptorIndex;
    }

    public String getDescription(ConstantPoolEntry[] constantPool) {
        return ConstantPoolValueRetriever.getString(constantPool, descriptorIndex);
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
        super.write(dout);
        dout.writeInt(descriptorIndex);
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public static class Factory extends ConstantPoolEntry.Factory {
        protected int descriptorIndex;

        @Override
        public void read(int tag, DataInputStream din) throws IOException {
            super.read(tag, din);
            descriptorIndex = din.readInt();
        }

        @Override
        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntryMethodType(tag, descriptorIndex);
        }
    }
}
