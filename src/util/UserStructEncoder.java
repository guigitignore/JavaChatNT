package util;
import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleEntry;

public class UserStructEncoder {
    private static UserStructEncoder instance=new UserStructEncoder();

    public static UserStructEncoder getInstance(){
        return instance;
    }

    public byte[] encode(String username,ClientType type){
        byte[] nameBytes=username.getBytes();
        int nameLength=nameBytes.length&0xFF;

        ByteBuffer wrapper=ByteBuffer.allocate(nameLength+2);
        wrapper.put((byte)nameLength);
        wrapper.put(nameBytes,0,nameLength);
        wrapper.put(type==ClientType.PACKETCHAT_CLIENT?(byte)0x1:(byte)0x0);
        return wrapper.array();
    }

    public byte[] encode(SimpleEntry<String,ClientType> entry){
        return encode(entry.getKey(),entry.getValue());
    }

    public SimpleEntry<String,ClientType> decode(byte[] data) throws NoSuchFieldException{
        ByteBuffer wrapper=ByteBuffer.wrap(data);
        if (wrapper.remaining()==0) throw new NoSuchFieldException("missing data");
        int nameLength=wrapper.get()&0xFF;
        if (wrapper.remaining()<nameLength+1) throw new NoSuchFieldException("insufficiant size for data");

        byte[] nameBytes=new byte[nameLength];
        wrapper.get(nameBytes);
        ClientType clientType=wrapper.get()==0x1?ClientType.PACKETCHAT_CLIENT:ClientType.TELNET_CLIENT;

        return new SimpleEntry<>(new String(nameBytes),clientType);
    }
}
