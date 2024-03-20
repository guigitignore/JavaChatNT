package util;
import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;

public class NounceManager {
    private HashMap<Byte,SimpleEntry<String,Boolean>> nounces=new HashMap<>();

    public boolean registerNounce(byte nounce,String username){
        synchronized(nounces){
            return username!=null && nounces.putIfAbsent(nounce, new SimpleEntry<String,Boolean>(username,false))==null;
        }
    }

    public Byte generateNounce(String username){
        Byte result=null;
        SimpleEntry<String,Boolean> entry=new SimpleEntry<String,Boolean>(username,false);

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
            SimpleEntry<String,Boolean> entry=nounces.get(nounce);
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
            SimpleEntry<String,Boolean> entry=nounces.get(nounce);
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
}
