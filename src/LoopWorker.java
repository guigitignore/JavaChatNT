public abstract class LoopWorker extends Thread implements IWorker{
    private Object[] args;

    protected Object[] getArgs(){
        return args;
    }

    public LoopWorker(Object...args){
        this.args=args;
        try{
            setup();
            WorkerManager.getInstance().registerAndStart(this);
        }catch(Exception e){}
    }

    public boolean getStatus() {
        return !isInterrupted();
    }

    public void cancel() {
        interrupt();
    }

    public abstract void setup() throws Exception;

    public abstract void init() throws Exception;

    public abstract void loop() throws Exception;

    public abstract void cleanup() throws Exception;

    public final void run(){
        try{
            init();
            
            while(true){
                try{
                    loop();
                }catch(Exception e2){
                    break;
                }
            }
        }catch(Exception e){}

        try{
            cleanup();
        }catch(Exception e){}

        WorkerManager.getInstance().remove(this);
    }
}
