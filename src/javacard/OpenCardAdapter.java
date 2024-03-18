package javacard;

import java.security.interfaces.RSAPublicKey;

import opencard.core.service.CardRequest;
import opencard.core.service.SmartCard;
import opencard.core.terminal.APDU;
import opencard.core.terminal.CommandAPDU;
import opencard.core.terminal.ResponseAPDU;
import opencard.core.util.HexString;
import opencard.opt.util.PassThruCardService;
import util.Logger;

public class OpenCardAdapter implements IJavacardInterface{

    private SmartCard smartcard;
    private PassThruCardService servClient;

    public OpenCardAdapter() throws Exception{
        CommandAPDU cmd;
        ResponseAPDU resp;

        SmartCard.start();
        Logger.i( "Initializing OpenCardAdapter... " ); 
        CardRequest cr = new CardRequest (CardRequest.ANYCARD,null,null); 
        smartcard = SmartCard.waitForCard (cr);

        if (smartcard==null) throw new Exception("Cannot find card");
        Logger.i( "Smartcard inserted" );
        Logger.i( "ATR: %s",HexString.hexify(smartcard.getCardID().getATR()));

        servClient = (PassThruCardService)smartcard.getCardService( PassThruCardService.class, true );

        cmd = new CommandAPDU( new byte[] {
            (byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00, (byte)0x0A,
            (byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x62, 
            (byte)0x03, (byte)0x01, (byte)0x0C, (byte)0x06, (byte)0x01
        } );

        resp = this.sendAPDU( cmd );
        
        if(!apdu2string( resp ).equals( "90 00" )) throw new Exception("Cannot select applet");
    }

    private ResponseAPDU sendAPDU(CommandAPDU apdu) throws Exception{
        ResponseAPDU result=this.servClient.sendCommandAPDU(apdu);
        Logger.i("--> Term: %s",apdu2string(apdu));
        Logger.i("<-- Card: %s",apdu2string(result));
        return result;
    }

    private String apdu2string(APDU apdu){
        return HexString.hexify( apdu.getBytes()).replace('\n', ' ');
    }


    public void select(String username) throws Exception {
        throw new Exception("Cannot select user");
    }

    public String getSelectedUser() {
        return null;
    }

    public RSAPublicKey getPublicKey() {
        return null;
    }

    public void clearUser() {
        
    }

    public byte[] solveChallenge(byte[] challenge) throws Exception {
        throw new Exception("Cannot solve challenge");
    }

    public byte[] encryptDES(byte[] data) throws Exception {
        throw new Exception("Cannot encrypt DES");
    }

    public byte[] decryptDES(byte[] data) throws Exception {
        throw new Exception("Cannot decrypt DES");
    }

    public void close() throws Exception {
        SmartCard.shutdown();
    }
    
}
