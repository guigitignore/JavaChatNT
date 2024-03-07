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

    public abstract void end() throws Exception;

    public final void run(){
        boolean initSuccess;

        try{
            init();
            initSuccess=true;
        }catch(Exception e){
            initSuccess=false;
        }

        if (initSuccess){
            while(true){
                try{
                    loop();
                }catch(Exception e){
                    break;
                }
            }
        }

        try{
            end();
        }catch(Exception e){}

        WorkerManager.getInstance().remove(this);
    }
}
