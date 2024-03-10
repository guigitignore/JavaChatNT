import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.AbstractMap.SimpleEntry;

public class RSAEncoder {
    private static RSAEncoder instance=null;

    public static RSAEncoder getInstance() throws NoSuchAlgorithmException{
        if (instance==null) instance=new RSAEncoder();
        return instance;
    }

    private KeyFactory keyFactory=null;

    private RSAEncoder() throws NoSuchAlgorithmException{
        keyFactory=KeyFactory.getInstance("RSA");
        
    }

    public byte[] encode(Key key) throws KeyException{
        BigInteger exponent;
        BigInteger modulus;

        if (key instanceof RSAPublicKey){
            modulus=((RSAPublicKey)key).getModulus();
            exponent=((RSAPublicKey)key).getPublicExponent();
        }else if (key instanceof RSAPrivateKey){
            modulus=((RSAPrivateKey)key).getModulus();
            exponent=((RSAPrivateKey)key).getPrivateExponent();
        }else{
            throw new KeyException("unsupported key type");
        }

        byte[] modulusBytes=modulus.toByteArray();
        if (modulusBytes.length>0xFF) throw new KeyException("Modulus is too large");

        byte[] exponentBytes=exponent.toByteArray();
        if (exponentBytes.length>0xFF) throw new KeyException("Exponent is too large");

        ByteBuffer buffer=ByteBuffer.allocate(modulusBytes.length+exponentBytes.length+2);
        buffer.put((byte)exponentBytes.length);
        buffer.put((byte)modulusBytes.length);
        buffer.put(exponentBytes);
        buffer.put(modulusBytes);

        return buffer.array();
    }

    private SimpleEntry<BigInteger,BigInteger> readData(byte[] data) throws InvalidKeySpecException{
        if (data.length<2){
            throw new InvalidKeySpecException("Insufficiant length");
        }
        ByteBuffer buffer=ByteBuffer.wrap(data);
        int exponentLength=(buffer.get()&0xFF);
        int modulusLength=(buffer.get()&0xFF);

        if (buffer.remaining()<exponentLength+modulusLength) throw new InvalidKeySpecException("Insufficiant length");
        byte[] exponentBytes=new byte[exponentLength];
        buffer.get(exponentBytes);

        byte[] modulusBytes=new byte[modulusLength];
        buffer.get(modulusBytes);

        return new SimpleEntry<BigInteger,BigInteger>(new BigInteger(1,exponentBytes),new BigInteger(1,modulusBytes));
        
    }

    public RSAPublicKey publicDecode(byte[] data) throws InvalidKeySpecException{
        SimpleEntry<BigInteger,BigInteger> pair=readData(data);
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(pair.getValue(),pair.getKey());
        return (RSAPublicKey)keyFactory.generatePublic(publicKeySpec);
    }

    public RSAPrivateKey privateDecode(byte[] data) throws InvalidKeySpecException{
        SimpleEntry<BigInteger,BigInteger> pair=readData(data);
        RSAPrivateKeySpec publicKeySpec = new RSAPrivateKeySpec(pair.getValue(),pair.getKey());
        return (RSAPrivateKey)keyFactory.generatePrivate(publicKeySpec);
    }

    
}
