public class PacketChatSanitizer {
    private ServiceChat client;

    public PacketChatSanitizer(ServiceChat client){
        this.client=client;
    }

    private void sanitizeAuthPacket(PacketChat packet) throws PacketChatException{
        if (packet.getFieldsNumber()==0){
            throw new PacketChatException("missing mandatory field in auth packet");
        }
    }

    private void sanitizeChallengePacket(PacketChat packet) throws PacketChatException{
        if (packet.getFieldsNumber()==0){
            packet.addField(new byte[0]);
        }
    }

    private void sanitizeMessagePacket(PacketChat packet) throws PacketChatException{
        String senderName;
        int fieldsNumber=packet.getFieldsNumber();

        if (fieldsNumber<2){
            throw new PacketChatException("Insuficiant arguments number");
        }
        switch (client.getUser().getTag()){
            case User.ADMIN_TAG:
                senderName=String.format("{%s}", client.getUser().getName());
                break;
            case User.USER_TAG:
                senderName=String.format("<%s>", client.getUser().getName());
                break;
            default:
                senderName="%unknown%";
                break;
        }
        packet.replaceField(0, senderName.getBytes());
    }

    private void sanitizeListUserPacket(PacketChat packet) throws PacketChatException{
        packet.clearFields();
    }


    private void logoutSanitize(PacketChat packet) throws PacketChatException{
        switch(packet.getCommand()){
            case PacketChat.AUTH:
                sanitizeAuthPacket(packet);
                break;
            case PacketChat.CHALLENGE:
                sanitizeChallengePacket(packet);
                break;
            default:
                throw new PacketChatException("Unauthorized packet type");
        }
    }


    private void loginSanitize(PacketChat packet) throws PacketChatException{
        switch(packet.getCommand()){
            case PacketChat.SEND_MSG:
                sanitizeMessagePacket(packet);
                break;
            case PacketChat.LIST_USERS:
                sanitizeListUserPacket(packet);
                break;
            default:
                throw new PacketChatException("Unknown packet type");
        }
    }

    public void sanitize(PacketChat packet) throws PacketChatException{
        if (client.getUser()==null){
            logoutSanitize(packet);
        }else{
            loginSanitize(packet);
        }
    }
}
