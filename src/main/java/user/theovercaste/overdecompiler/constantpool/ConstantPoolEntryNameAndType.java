package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntryNameAndType extends ConstantPoolEntry {
    protected final int nameIndex;
    protected final int descriptorIndex;

    public ConstantPoolEntryNameAndType(int tag, int nameIndex, int descriptorIndex) {
        super(tag);
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
    }

    public int getNameIndex( ) {
        return nameIndex;
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public String getName(ConstantPoolEntry[] constantPool) {
        return ConstantPoolValueRetriever.getString(constantPool, nameIndex);
    }

    public String getDescription(ConstantPoolEntry[] constantPool) {
        return ConstantPoolValueRetriever.getString(constantPool, descriptorIndex);
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
        super.write(dout);
        dout.writeShort(nameIndex);
        dout.writeShort(descriptorIndex);
    }

    public static class Factory extends ConstantPoolEntry.Factory {
        protected int nameIndex;
        protected int descriptorIndex;

        @Override
        public void read(int tag, DataInputStream din) throws IOException {
            super.read(tag, din);
            nameIndex = din.readUnsignedShort();
            descriptorIndex = din.readUnsignedShort();
        }

        @Override
        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntryNameAndType(tag, nameIndex, descriptorIndex);
        }
    }
}
