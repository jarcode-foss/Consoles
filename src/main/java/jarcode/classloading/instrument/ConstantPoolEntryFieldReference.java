package jarcode.classloading.instrument;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/*
 * Originally written by <a href="http://github.com/overcaste">overcaste</a>.
 */
public class ConstantPoolEntryFieldReference extends ConstantPoolEntry {
    protected final int classIndex;
    protected final int nameAndTypeIndex;

    public ConstantPoolEntryFieldReference(int tag, int classIndex, int nameAndTypeIndex) {
        super(tag);
        this.classIndex = classIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
    }

    public int getClassIndex( ) {
        return classIndex;
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

    public String getClassName(ConstantPoolEntry[] constantPool) {
        return ConstantPoolValueRetriever.getClassName(constantPool, classIndex);
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
        super.write(dout);
        dout.writeShort(classIndex);
        dout.writeShort(nameAndTypeIndex);
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public static class Factory extends ConstantPoolEntry.Factory {
        protected int classIndex;
        protected int nameAndTypeIndex;

        @Override
        public void read(int tag, DataInputStream din) throws IOException {
            super.read(tag, din);
            classIndex = din.readUnsignedShort();
            nameAndTypeIndex = din.readUnsignedShort();
        }

        @Override
        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntryFieldReference(tag, classIndex, nameAndTypeIndex);
        }
    }
}
