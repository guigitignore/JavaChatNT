public class PacketChatSanitizer {
    public PacketChatSanitizer(PacketChat packet,ServiceChat user) throws PacketChatException{
        switch(packet.getCommand()){
            case PacketChat.SEND_MSG:
                int fieldsNumber=packet.getFieldsNumber();
                if (fieldsNumber<2){
                    throw new PacketChatException("Insuficiant arguments number");
                }
                packet.replaceField(0, user.getUserName().getBytes());
        }
    }
}
