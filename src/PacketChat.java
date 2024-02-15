import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PacketChat {

    public final static byte STATUS_SUCCESS=0;
    public final static byte STATUS_ERROR=1;

    private final static int HEADER_SIZE=8;
    private final static int MAX_DATA_SIZE=0x100000;
    
    private byte cmd;
    private byte status;
    private byte flag;
    private byte param;
    private ArrayList<byte[]> fields=new ArrayList<>();

    public void receive(InputStream stream) throws PacketChatException{
        
        byte[] headerBytes=new byte[HEADER_SIZE];
        try{
            stream.read(headerBytes);
        }catch(IOException e){
            throw new PacketChatException("Cannot read packet header");
        }
        

        ByteBuffer header=ByteBuffer.wrap(headerBytes);

        setCommand(header.get());
        setStatus(header.get());
        setFlag(header.get());
        setParam(header.get());
        
        int dataSize=header.getInt();
        if (dataSize>MAX_DATA_SIZE) throw new PacketChatException("Data section is too big");

        byte[] dataBytes=new byte[dataSize];

        try{
            stream.read(dataBytes);
        }catch(IOException e){
            throw new PacketChatException("Cannot read packet data");
        }

        //reset fields
        fields.clear();
        ByteBuffer data=ByteBuffer.wrap(dataBytes);

        while (data.remaining()>2){
            int fieldSize=(data.getShort()&0xFFFF);

            if (fieldSize>data.remaining()){
                throw new PacketChatException(String.format("Malformet packet field %d", fields.size()));
            }

            //end of data
            if (fieldSize==0) break;

            byte[] field=new byte[fieldSize];
            data.get(field);
            fields.add(field);
        }
        
    }

    public void send(OutputStream stream) throws PacketChatException{
        int fieldsSize=0;
        for (byte[] field:fields) fieldsSize+=field.length+Short.BYTES;

        byte[] packetBytes=new byte[HEADER_SIZE+fieldsSize];
        ByteBuffer packet=ByteBuffer.wrap(packetBytes);

        packet.put(getCommand());
        packet.put(getStatus());
        packet.put(getFlag());
        packet.put(getParam());
        packet.putInt(fieldsSize);

        for (byte[] field:fields){
            packet.putShort((short)field.length);
            packet.put(field);
        }

        try{
            stream.write(packetBytes);
        }catch(IOException e){
            throw new PacketChatException("Cannot send packet");
        }
        
    }

    public PacketChat(){
    }

    public void setCommand(byte command){
        this.cmd=command;
    }

    public byte getCommand(){
        return cmd;
    }

    public void setStatus(byte status){
        this.status=status;
    }

    public byte getStatus(){
        return status;
    }

    public void setFlag(byte status){
        this.status=status;
    }

    public byte getFlag(){
        return flag;
    } 

    public void setParam(byte param){
        this.param=param;
    }

    public byte getParam(){
        return param;
    } 

    public int getFieldsNumber(){
        return fields.size();
    }

    public byte[][] getFields(){
        return fields.toArray(new byte[fields.size()][]);
    }

    public byte[] getField(int index){
        return fields.get(index);
    }

    public byte[] removeField(int index){
        return fields.remove(index);
    }

    public void addField(byte[] field){
        fields.add(field);
    }
}
