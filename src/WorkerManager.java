import java.util.ArrayList;

public class WorkerManager {
    private ArrayList<IWorker> workers=new ArrayList<>();

    private final static WorkerManager instance=new WorkerManager();

    public static WorkerManager getInstance(){
        return instance;
    } 

    public void register(IWorker worker){
        Logger.i(String.format("new worker : %s", worker.getDescription()));

        synchronized(workers){
            workers.add(worker);
        }
    }

    public void registerAndStart(IWorker worker){
        register(worker);
        worker.start();
    }

    public boolean cancel(int index){
        boolean status=false;
        IWorker worker;

        if (index>=0 && index<workers.size()){
            synchronized(workers){
                worker=workers.get(index);
            }
            worker.cancel();
            status=!worker.getStatus();
        }
        return status;
    }

    public void cancelAll(){
        for (IWorker worker:getWorkers()){
            worker.cancel();
        }
    }

    public IWorker[] getWorkers(){
        synchronized(workers){
            return workers.toArray(new IWorker[workers.size()]);
        }
    }

    public void remove(IWorker worker){
        Logger.i(String.format("removing worker : %s", worker.getDescription()));

        synchronized(workers){
            workers.remove(worker);
        }
    }
}
