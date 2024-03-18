package javacard;

import java.security.interfaces.RSAPublicKey;

public interface IJavacardInterface {

    public void select(String username) throws Exception;
    
    public String getSelectedUser();

    public RSAPublicKey getPublicKey();

    public void clearUser();

    public byte[] solveChallenge(byte[] challenge) throws Exception;

    public byte[] encryptDES(byte[] data) throws Exception;

    public byte[] decryptDES(byte[] data) throws Exception;
}