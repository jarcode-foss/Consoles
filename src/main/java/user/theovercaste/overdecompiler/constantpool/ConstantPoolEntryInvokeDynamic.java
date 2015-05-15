package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntryInvokeDynamic extends ConstantPoolEntry {
    protected final int methodAttributeIndex;
    protected final int nameAndTypeIndex;

    public ConstantPoolEntryInvokeDynamic(int tag, int methodAttributeIndex, int nameAndTypeIndex) {
        super(tag);
        this.methodAttributeIndex = methodAttributeIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
    }

    public int getMethodAttributeIndex( ) {
        return methodAttributeIndex;
    }

    public int getNameAndTypeIndex( ) {
        return nameAndTypeIndex;
    }

    public String getName(ConstantPoolEntry[] constantPool) {
        return ConstantPoolValueRetriever.getNameAndTypeName(constantPool, nameAndTypeIndex);
    }

    public String getDescription(ConstantPoolEntry[] constantPool) {
        return ConstantPoolValueRetriever.getNameAndTypeDescription(constantPool, nameAndTypeIndex);
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
        super.write(dout);
        dout.writeShort(methodAttributeIndex);
        dout.writeShort(nameAndTypeIndex);
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public static class Factory extends ConstantPoolEntry.Factory {
        protected int methodAttributeIndex;
        protected int nameAndTypeIndex;

        @Override
        public void read(int tag, DataInputStream din) throws IOException {
            super.read(tag, din);
            methodAttributeIndex = din.readUnsignedShort();
            nameAndTypeIndex = din.readUnsignedShort();
        }

        @Override
        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntryInvokeDynamic(tag, methodAttributeIndex, nameAndTypeIndex);
        }
    }
}
