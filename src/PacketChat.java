import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class PacketChat {
    public final static byte AUTH =               (byte)0x00;
    public final static byte CHALLENGE =          (byte)0x01;
    public final static byte EXIT =               (byte)0x02;
    public final static byte SEND_MSG =           (byte)0x03;
    public final static byte LIST_USERS =         (byte)0x04;
    public final static byte FILE_INIT =          (byte)0x05;
    public final static byte FILE_DATA =          (byte)0x06;
    public final static byte FILE_OVER =          (byte)0x07;
    public final static byte HELLO =              (byte)0xFF;

    public final static byte STATUS_SUCCESS=      (byte)0x00;
    public final static byte STATUS_ERROR=        (byte)0x01;


    private final static int HEADER_SIZE=8;
    private final static int MAX_DATA_SIZE=0x100000;
    
    private byte cmd;
    private byte status;
    private byte flag;
    private byte param;
    private ArrayList<byte[]> fields=new ArrayList<>();

    public PacketChat(InputStream stream) throws PacketChatException{
        
        byte[] headerBytes=new byte[HEADER_SIZE];
        try{
            int bytesRead=stream.read(headerBytes);
            if (bytesRead!=HEADER_SIZE) throw new IOException();
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

    @Override
    public String toString(){
        return String.format("PacketChat<command=%d|status=%d|param=%d|flag=%d|fields=%d>", 
                getCommand(),getStatus(),getParam(),getFlag(),getFieldsNumber());
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

    public void setFlag(byte flag){
        this.flag=flag;
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

    public byte[] replaceField(int index,byte[] value){
        return fields.set(index,value);
    }

    public byte[] removeField(int index){
        return fields.remove(index);
    }

    public void addField(byte[] field){
        fields.add(field);
    }
}
