import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class InterruptibleInputStream extends FileInputStream{
    
    public InterruptibleInputStream(){
        super(FileDescriptor.in);
    }

    public int read(byte[] buff, int offset, int length) throws IOException{
        int availableBytes;
        try{
            while (true){
                availableBytes=super.available();
                if (availableBytes>0){
                    return super.read(buff, offset, availableBytes);
                }
                Thread.sleep(10);
            }
        }catch(InterruptedException e){
            throw new IOException(e.getMessage());
        }
    }
}
