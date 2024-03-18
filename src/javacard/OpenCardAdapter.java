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

    private final static int CHUNK_SIZE=248;
    private final static int DES_KEY_SIZE=8;

    public static final byte CLA		    =0x00;
	public static final byte DES_ENCRYPT    =0x01;
	public static final byte DES_DECRYPT    =0x02;

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
        
        if(getApduCode(resp)!=0x9000) throw new Exception("Cannot select applet");
    }

    private ResponseAPDU sendAPDU(CommandAPDU apdu) throws Exception{
        try{
            ResponseAPDU result=this.servClient.sendCommandAPDU(apdu);
            Logger.i("--> Term: %s",apduToString(apdu));
            Logger.i("<-- Card: %s",apduToString(result));
            return result;
        }catch(Exception e){
            throw new Exception(String.format("Cannot send APDU: %s",apduToString(apdu)));
        }
    }

    private String apduToString(APDU apdu){
        return HexString.hexify( apdu.getBytes()).replace('\n', ' ');
    }

    private int getApduCode(APDU apdu){
		byte[] data=apdu.getBytes();
		if (data.length<2) return -1;
		return (data[data.length-2]&0xFF)<<8 | 	(data[data.length-1]&0xFF);
	}

    private String getApduErrorFromCode(int code){
        String result;

		switch (code){
			case 0x9000:
				result="No APDU Error";
                break;
			case 0x6D00:
                result="Command not supported";
                break;
			case 0x6F00:
                result="APDU command aborted. Internal error";
                break;
			case -1:
				result="Invalid APDU reponse length";
                break;
			default:
				result="Unknown APDU error";
		}

        return result;
	}

    byte[] sendCommand(byte commandId,byte p1,byte p2,byte[] data,byte expectedLength) throws Exception{
		//build command
		byte[] header={CLA,commandId,p1,p2,(byte)(data.length&0xFF)};
		byte[] command;

		//if exepected length is 0, do not send it
		if (expectedLength==(byte)0){
			command=new byte[data.length+header.length];
		}else{
			command=new byte[data.length+header.length+1];
			command[command.length-1]=(byte)(expectedLength&0xFF);
		}
		 

		System.arraycopy(header,0,command,0,header.length);
		System.arraycopy(data,0,command,header.length,data.length);
			
	    CommandAPDU cmd = new CommandAPDU( command );
        ResponseAPDU resp = this.sendAPDU( cmd );
		int code=getApduCode(resp);
	    
		//if apdu is not OK get the error message and throw exception
		if (code!=0x9000) throw new Exception(getApduErrorFromCode(code));
		//get bytes
		byte[] respData=resp.getBytes();
		byte[] resultData=new byte[respData.length-2];
		//remove response code
		System.arraycopy(respData,0,resultData,0,resultData.length);
		//return bytes
		return resultData;
	}

    private byte[] pad(byte[] data){
        int toAdd=DES_KEY_SIZE-(data.length%DES_KEY_SIZE);
        byte[] result=new byte[data.length+toAdd];
        System.arraycopy(data, 0, result, 0, data.length);
        for (int i=0;i<toAdd;i++) result[data.length+i]=(byte)toAdd;
        return result;
    }

    private byte[] unpad(byte[] data){
        int resultLength=data.length-data[data.length-1];
        byte[] result=new byte[resultLength];
        System.arraycopy(data, 0, result, 0, resultLength);
        return result;
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
        data=pad(data);
        return sendCommand(DES_ENCRYPT,(byte)0,(byte)0,data,(byte)data.length) ;
    }

    public byte[] decryptDES(byte[] data) throws Exception {
        return unpad(sendCommand(DES_DECRYPT,(byte)0,(byte)0,data,(byte)data.length));
    }

    public void close() throws Exception {
        SmartCard.shutdown();
    }
    
}
