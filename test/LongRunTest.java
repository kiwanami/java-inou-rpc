import inou.net.rpc.BinClient;
import inou.net.rpc.IMessageHandler;
import inou.net.rpc.MultiBinServer;

public class LongRunTest {

    private static int count = 0;
    private static int max = 1024;
    
    private static MultiBinServer server;
    
    private static int startServer() throws Exception {
        server = new MultiBinServer();
        server.addHandler("c2s",new IMessageHandler() {
            public Object send(Object[] arg) throws Exception {
                return "OK:"+arg[0];
            }
        });
        new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        server.getClientConnection();
                    }
                } catch (Throwable e) {
                    System.out.println("SError:"+count);
                    e.printStackTrace();
                }
            }
        }).start();
        return server.getPortNumber();
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("$ java LongRunTest (host) (port)");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        try {
            for(;count<max;count++) {
                BinClient client = new BinClient(host,port);
                client.start();
                client.send("echo_string",new Object[]{"Hello:"+count});
                client.shutdown();
                System.out.print(".");
                if (count % 40 == 0) {
                    System.out.println(" "+count);
                }
            }
        } catch (Throwable e) {
            System.out.println("CError:"+count);
            e.printStackTrace();
        }
    }

}
