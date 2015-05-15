package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntryClass extends ConstantPoolEntry {
    protected final int nameIndex;

    public ConstantPoolEntryClass(int tag, int nameIndex) {
        super(tag);
        this.nameIndex = nameIndex;
    }

    public int getNameIndex( ) {
        return nameIndex;
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
        super.write(dout);
        dout.writeShort(nameIndex);
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public String getName(ConstantPoolEntry[] constantPool) {
        return ConstantPoolValueRetriever.getString(constantPool, nameIndex);
    }

    public static class Factory extends ConstantPoolEntry.Factory {
        protected int nameIndex;

        @Override
        public void read(int tag, DataInputStream din) throws IOException {
            super.read(tag, din);
            nameIndex = din.readUnsignedShort();
        }

        @Override
        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntryClass(tag, nameIndex);
        }
    }
}
