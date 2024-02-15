import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;

public class MultipleOutputStream extends OutputStream{
    private final static MultipleOutputStream instance=new MultipleOutputStream();
    private HashSet<OutputStream> streams=new HashSet<>();

    private MultipleOutputStream(){}

    public static MultipleOutputStream getInstance(){
        return instance;
    }


    public boolean add(OutputStream stream){
        boolean status;
        if (stream!=null){
            synchronized(streams){
                status=streams.add(stream);
            } 
        }
        else status=false;
        return status;
    }

    public boolean remove(OutputStream stream){
        boolean status;
        if (stream!=null){
            synchronized(streams){
                status=streams.remove(stream);
            }
        }
        else status=false;
        return status;
    }

    public void write(int arg0){
        synchronized(streams){
            for (OutputStream stream:streams){
                try{
                    stream.write(arg0);
                }catch(IOException e){}
            }
        }
    }
    
}
