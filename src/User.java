public abstract class User {
    public final static String USER_TAG="USER";
    public final static String ADMIN_TAG="ADMIN";

    protected byte[] key;
    protected String name;
    private String tag;

    public User(String name,byte[] key,String tag){
        this.key=key;
        this.name=name;
        this.tag=tag.strip().toUpperCase();
    }

    public User(String name,byte[] key){
        this(name,key,USER_TAG);
    }

    public abstract String getTypeName();

    public abstract IChallenge getChallenge();

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
