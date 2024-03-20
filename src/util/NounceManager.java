package util;
import java.util.HashMap;
import java.util.Iterator;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

public class NounceManager implements Iterable<Entry<Byte,Entry<String,Boolean>>>{
    private HashMap<Byte,Entry<String,Boolean>> nounces=new HashMap<>();

    public boolean registerNounce(byte nounce,String username){
        synchronized(nounces){
            return username!=null && nounces.putIfAbsent(nounce, new SimpleEntry<String,Boolean>(username,false))==null;
        }
    }

    public Byte generateNounce(String username){
        Byte result=null;
        Entry<String,Boolean> entry=new SimpleEntry<String,Boolean>(username,false);

        synchronized(nounces){
            for (int i=0;i<256;i++){
                if (nounces.putIfAbsent((byte)i, entry)==null){
                    Logger.i("Generate nounce=%d",i);
                    result=(byte)i;
                    break;
                }
            }
        }
        return result;
    }

    public boolean allowNounce(byte nounce,String username){
        boolean result=false;
        synchronized(nounces){
            Entry<String,Boolean> entry=nounces.get(nounce);
            if (entry!=null && entry.getKey().equals(username)){
                entry.setValue(true);
                nounces.replace(nounce, entry);
                result=true;
            }
        }
        return result;
    }

    public boolean allowNounce(byte nounce){
        return allowNounce(nounce,getUsernameFromNounce(nounce));
    }

    public boolean isNounceAllowed(byte nounce,String username){
        synchronized(nounces){
            Entry<String,Boolean> entry=nounces.get(nounce);
            return entry!=null && entry.getKey().equals(username) && entry.getValue()==true;
        }
    }

    public boolean isNounceAllowed(byte nounce){
        return isNounceAllowed(nounce, getUsernameFromNounce(nounce));
    }

    public void removeNounce(byte nounce){
        synchronized(nounces){
            nounces.remove(nounce);
        }
    }

    public boolean isNounceDefined(byte nounce){
        synchronized(nounces){
            return nounces.containsKey(nounce);
        }
    }

    public String getUsernameFromNounce(byte nounce){
        String result=null;
        synchronized(nounces){
            if (nounces.containsKey(nounce)){
                result=nounces.get(nounce).getKey();
            }
        }
        return result;
    }
    
    public Iterator<Entry<Byte,Entry<String,Boolean>>> iterator(){
        synchronized(nounces){
            return nounces.entrySet().iterator();
        }  
    }

    public String toString(){
        StringBuilder builder=new StringBuilder();
        
        for (Entry<Byte,Entry<String,Boolean>> nounce:this){
            Entry<String,Boolean> value=nounce.getValue();
            builder.append(String.format("nounce=%d user=\"%s\" allowed=%b\n",nounce.getKey(),value.getKey(),value.getValue()));
        }
        return builder.toString();
    }
    
}
