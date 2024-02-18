public class PacketChatSanitizer {
    private ServiceChat user;

    public PacketChatSanitizer(ServiceChat user){
        this.user=user;
    }

    public void logoutSanitize(PacketChat packet) throws PacketChatException{
        switch(packet.getCommand()){
            case PacketChat.AUTH:
                if (packet.getFieldsNumber()==0){
                    throw new PacketChatException("missing mandatory field in auth packet");
                }
                break;
            case PacketChat.CHALLENGE:
                if (packet.getFieldsNumber()==0){
                    packet.addField(new byte[0]);
                }
                break;
            default:
                throw new PacketChatException("Unauthorized packet type");
        }
    }

    public void loginSanitize(PacketChat packet) throws PacketChatException{
        switch(packet.getCommand()){
            case PacketChat.SEND_MSG:
                int fieldsNumber=packet.getFieldsNumber();
                if (fieldsNumber<2){
                    throw new PacketChatException("Insuficiant arguments number");
                }
                packet.replaceField(0, user.getUser().getName().getBytes());
                break;
            case PacketChat.LIST_USERS:
                packet.clearFields();
                packet.addField(user.getUser().getName().getBytes());
                break;
            default:
                throw new PacketChatException("Unknown packet type");
        }
    }

    public void sanitize(PacketChat packet) throws PacketChatException{
        if (user.getUser()==null){
            logoutSanitize(packet);
        }else{
            loginSanitize(packet);
        }
    }
}
