class Main{
    public static void main(String[] args) {
        Logger.i("Starting engine...");
        new ServerChat(2000,User.USER_TAG);
        new ServerChat(2001,User.ADMIN_TAG);
        new ServerTelnet(3000,2000);
        new ServerTelnet(3001,2001);

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