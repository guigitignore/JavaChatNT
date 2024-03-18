package packetchat;
public class PacketChatException extends Exception {
    public PacketChatException(String message){
        super(message);
    }

    public PacketChatException(String format,Object...args){
        this(String.format(format, args));
    }
}
