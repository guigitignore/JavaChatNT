class Main{
    public static void main(String[] args) {
        Logger.i("Starting engine...");
        new ServerManager();
        new ServerChat();
        new ServerTelnet();

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            Logger.w("Trigger engine interruption...");
            WorkerManager.getInstance().cancelAll();
            //wait 100ms
            try{
                Thread.sleep(100); //let time to logger to send messages to output
            }catch(InterruptedException e){}
            
        }));
        
        //WorkerManager.getInstance().registerAndStart(new LocalClientManager());
    }
}