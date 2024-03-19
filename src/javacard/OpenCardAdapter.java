package javacard;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

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

    private RSAPublicKey publicKey=null;
    private String username=null;

    private final static int CHUNK_SIZE=248;
    private final static int DES_KEY_SIZE=8;

    public static final byte CLA		    =0x00;
	public static final byte DES_ENCRYPT    =0x01;
	public static final byte DES_DECRYPT    =0x02;
    public static final byte RSA_GET_MODULUS         =0x03;
	public static final byte RSA_GET_PUBLIC_EXPONENT =0x04;
	public static final byte RSA_DECRYPT             =0x05;

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
            case 0x6700:
                result="Invalid APDU command length";
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
        if (data.length>255) throw new Exception("Data too long");

        int commandLength=expectedLength!=(byte)0 && data.length!=0?data.length+6:data.length+5;
        ByteBuffer buffer=ByteBuffer.allocate(commandLength);
        buffer.put(CLA);
        buffer.put(commandId);
        buffer.put(p1);
        buffer.put(p2);

        if (data.length>0){
            buffer.put((byte)(data.length&0xFF));
            buffer.put(data);

            if (buffer.hasRemaining()) buffer.put(expectedLength);
        }
        
	    CommandAPDU cmd = new CommandAPDU(buffer.array());
        ResponseAPDU resp = this.sendAPDU(cmd);
		int code=getApduCode(resp);
	    
		//if apdu is not OK get the error message and throw exception
		if (code!=0x9000) throw new Exception(getApduErrorFromCode(code));
		//get bytes
		byte[] respData=resp.getBytes();
		return Arrays.copyOf(respData, respData.length-2);
	}

    byte[] sendCommand(byte commandId) throws Exception{
        return sendCommand(commandId, (byte)0, (byte)0, new byte[0],(byte)0);
    }

    private byte[] pad(byte[] data,int padSize) throws Exception{
        if (padSize<0x00 || padSize>0xFF) throw new Exception("Invalid padding length"); 

        int toAdd=padSize-(data.length%padSize);
        int resultLength=data.length+toAdd;
        byte[] result=Arrays.copyOf(data,resultLength);
        Arrays.fill(result,data.length,resultLength,(byte)toAdd);
        return result;
    }

    private byte[] unpad(byte[] data) throws Exception{
        int resultLength=data.length-(data[data.length-1]&0xFF);
        if (resultLength<0) throw new Exception("Invalid padding length");
        return Arrays.copyOf(data,resultLength);
    }


    public void select(String username) throws Exception {
        KeyFactory keyFactory=KeyFactory.getInstance("RSA");

        BigInteger modulus=new BigInteger(sendCommand(RSA_GET_MODULUS));
        BigInteger exponent=new BigInteger(sendCommand(RSA_GET_PUBLIC_EXPONENT));

        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus,exponent);

        this.publicKey=(RSAPublicKey)keyFactory.generatePublic(publicKeySpec);
        this.username=username;
    }

    public String getSelectedUser() {
        return username;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public void clearUser() {
        this.username=null;
        this.publicKey=null;
    }

    public byte[] solveChallenge(byte[] challenge) throws Exception {
        return sendCommand(RSA_DECRYPT, (byte)0, (byte)0, challenge,(byte)challenge.length);
    }

    private byte[] desOperation(byte[] data,byte operation) throws Exception{
        byte[] chunk;
        byte[] result;
        ByteBuffer buffer=ByteBuffer.wrap(data);

        while (buffer.hasRemaining()){
            int pos=buffer.position();
            chunk=buffer.remaining()>CHUNK_SIZE?new byte[CHUNK_SIZE]:new byte[buffer.remaining()];
            buffer.get(chunk);
            buffer.position(pos);
            result=sendCommand(operation,(byte)0,(byte)0,chunk,(byte)chunk.length);

            if (result.length!=chunk.length) throw new Exception("Invalid result length");

            buffer.put(result);
        }
        return buffer.array();
    }

    public byte[] encryptDES(byte[] data) throws Exception {
        return desOperation(pad(data,DES_KEY_SIZE), DES_DECRYPT);
    }

    public byte[] decryptDES(byte[] data) throws Exception {
        return unpad(desOperation(data, DES_DECRYPT));
    }

    public void close() throws Exception {
        SmartCard.shutdown();
    }
    
}
