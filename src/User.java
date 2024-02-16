public abstract class User {
    protected byte[] key;
    protected String name;
    private String tag;

    public User(String name,byte[] key,String tag){
        this.key=key;
        this.name=name;
        this.tag=tag.strip().toUpperCase();
    }

    public User(String name,byte[] key){
        this(name,key,"USER");
    }

    public abstract String getTypeName();

    public byte[] getKey() {
        return key;
    }

    public String getName(){
        return name;
    }

    public String getTag(){
        return tag;
    }

}
