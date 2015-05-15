package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConstantPoolEntries {
    public static final int UTF8_TAG = 1;
    public static final int INTEGER_TAG = 3;
    public static final int FLOAT_TAG = 4;
    public static final int LONG_TAG = 5;
    public static final int DOUBLE_TAG = 6;
    public static final int CLASS_TAG = 7;
    public static final int STRING_TAG = 8;
    public static final int FIELD_REFERENCE_TAG = 9;
    public static final int METHOD_REFERENCE_TAG = 10;
    public static final int INTERFACE_METHOD_REFERENCE_TAG = 11;
    public static final int NAME_AND_TYPE_TAG = 12;
    public static final int METHOD_HANDLE_TAG = 15;
    public static final int METHOD_TYPE_TAG = 16;
    public static final int INVOKE_DYNAMIC_TAG = 18;

    private static final Map<Integer, ConstantPoolEntry.Factory> factories = new HashMap<>();

    static {
        factories.put(UTF8_TAG, ConstantPoolEntryUtf8.factory());
        factories.put(INTEGER_TAG, ConstantPoolEntryInteger.factory());
        factories.put(FLOAT_TAG, ConstantPoolEntryFloat.factory());
        factories.put(LONG_TAG, ConstantPoolEntryLong.factory());
        factories.put(DOUBLE_TAG, ConstantPoolEntryDouble.factory());
        factories.put(CLASS_TAG, ConstantPoolEntryClass.factory());
        factories.put(STRING_TAG, ConstantPoolEntryString.factory());
        factories.put(FIELD_REFERENCE_TAG, ConstantPoolEntryFieldReference.factory());
        factories.put(METHOD_REFERENCE_TAG, ConstantPoolEntryMethodReference.factory());
        factories.put(INTERFACE_METHOD_REFERENCE_TAG, ConstantPoolEntryInterfaceMethodReference.factory());
        factories.put(NAME_AND_TYPE_TAG, ConstantPoolEntryNameAndType.factory());
        factories.put(METHOD_HANDLE_TAG, ConstantPoolEntryMethodHandle.factory());
        factories.put(METHOD_TYPE_TAG, ConstantPoolEntryMethodType.factory());
        factories.put(INVOKE_DYNAMIC_TAG, ConstantPoolEntryInvokeDynamic.factory());
    }

    public static ConstantPoolEntry readEntry(DataInputStream din) throws IOException {
        try {
            int tag = din.readUnsignedByte();
            ConstantPoolEntry.Factory b = factories.get(tag);
            b.read(tag, din);
            return b.build();
        } catch (IOException e) {
            throw e;
        }
    }
}
