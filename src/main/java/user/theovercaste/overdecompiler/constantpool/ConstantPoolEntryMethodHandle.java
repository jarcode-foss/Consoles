package user.theovercaste.overdecompiler.constantpool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;

public class ConstantPoolEntryMethodHandle extends ConstantPoolEntry {
    public enum ReferenceType {
        GET_FIELD(1),
        GET_STATIC(2),
        PUT_FIELD(3),
        PUT_STATIC(4),
        INVOKE_VIRTUAL(5),
        INVOKE_STATIC(6),
        INVOKE_SPECIAL(7),
        NEW_INVOKE_SPECIAL(8),
        INVOKE_INTERFACE(9);

        public final int id;

        ReferenceType(int id) {
            this.id = id;
        }

        private static ImmutableBiMap<Integer, ReferenceType> map = ImmutableBiMap.<Integer, ReferenceType> builder() // Patented way to create a bimap of EnumValue <-> Integer
                .putAll(Maps.uniqueIndex(
                        Arrays.asList(ReferenceType.values()),
                        new Function<ReferenceType, Integer>() {
                            @Override
                            public Integer apply(ReferenceType input) {
                                return input.id;
                            }
                        }))
                .build();

        public static ReferenceType byId(int id) {
            return map.get(id);
        }
    }

    protected final int referenceKind;
    protected final int referenceIndex;

    public ConstantPoolEntryMethodHandle(int tag, int referenceKind, int referenceIndex) {
        super(tag);
        this.referenceKind = referenceKind;
        this.referenceIndex = referenceIndex;
    }

    public static Factory factory( ) {
        return new Factory();
    }

    public int getReferenceKind( ) {
        return referenceKind;
    }

    public int getReferenceIndex( ) {
        return referenceIndex;
    }

    @Override
    public void write(DataOutputStream dout) throws IOException {
        super.write(dout);
        dout.writeByte(referenceKind);
        dout.writeShort(referenceIndex);
    }

    public static class Factory extends ConstantPoolEntry.Factory {
        protected int referenceKind;
        protected int referenceIndex;

        @Override
        public void read(int tag, DataInputStream din) throws IOException {
            super.read(tag, din);
            referenceKind = din.readUnsignedByte();
            referenceIndex = din.readUnsignedShort();
        }

        @Override
        public ConstantPoolEntry build( ) {
            return new ConstantPoolEntryMethodHandle(tag, referenceKind, referenceIndex);
        }
    }
}
