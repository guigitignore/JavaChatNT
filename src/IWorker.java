public interface IWorker {
    
    public boolean getStatus();

    public String getDescription();

    public void start();

    public void cancel();
}
