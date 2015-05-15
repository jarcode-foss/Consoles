package user.theovercaste.overdecompiler.constantpool;

public class ConstantPoolValueRetriever {
    public static String getNameAndTypeName(ConstantPoolEntry[] constantPool, int index) {
        ConstantPoolEntry entry = constantPool[index];
        if (entry instanceof ConstantPoolEntryNameAndType) {
            return ((ConstantPoolEntryNameAndType) entry).getName(constantPool);
        }
        return null;
    }

    public static String getNameAndTypeDescription(ConstantPoolEntry[] constantPool, int index) {
        ConstantPoolEntry entry = constantPool[index];
        if (entry instanceof ConstantPoolEntryNameAndType) {
            return ((ConstantPoolEntryNameAndType) entry).getDescription(constantPool);
        }
        return null;
    }

    public static String getString(ConstantPoolEntry[] constantPool, int index) {
        ConstantPoolEntry entry = constantPool[index];
        if (entry instanceof ConstantPoolEntryUtf8) {
            return ((ConstantPoolEntryUtf8) entry).getValue();
        }
        if (entry instanceof ConstantPoolEntryString) {
            return getString(constantPool, ((ConstantPoolEntryString) entry).getStringIndex());
        }
        return null;
    }

    public static String getString(ConstantPoolEntry entry) {
        if (entry instanceof ConstantPoolEntryUtf8) {
            return ((ConstantPoolEntryUtf8) entry).getValue();
        }
        return null;
    }

    public static String getClassName(ConstantPoolEntry[] constantPool, int index) {
        ConstantPoolEntry entry = constantPool[index];
        if (entry instanceof ConstantPoolEntryClass) {
            return ((ConstantPoolEntryClass) entry).getName(constantPool);
        }
        return null;
    }
}
