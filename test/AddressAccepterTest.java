import java.io.IOException;
import java.net.InetAddress;

import inou.net.rpc.BinClient;
import inou.net.rpc.BinServer;
import inou.net.rpc.ICommunicator;
import inou.net.rpc.IConnectionAccepter;
import inou.net.rpc.IMessageHandler;
import inou.net.rpc.LocalhostAccepter;
import inou.net.rpc.MessageServer;
import inou.net.rpc.MultiBinServer;
import junit.framework.Assert;
import junit.framework.TestCase;

public class AddressAccepterTest extends TestCase {

    private BinServer server;
    private MultiBinServer multiServer;
    
    protected void setUp() throws Exception {
        IMessageHandler handler = new IMessageHandler() {
            public Object send(Object[] args) throws Exception {
                return args[0];
            }
        };
        server = new BinServer();
        server.addHandler("echo",handler);
        server.start();
        multiServer = new MultiBinServer();
        multiServer.addHandler("echo",handler);
        Thread thread = new Thread(new Runnable(){
            public void run() {
                while(true) {
                    try {
                        ICommunicator client = multiServer.getClientConnection();
                        if (client == null) break;
                    } catch (IOException e) {
                        break;
                    }
                }
            }});
        thread.start();
    }

    protected void tearDown() throws Exception {
        server.shutdown();
        multiServer.shutdown();
    }

    private IConnectionAccepter localAccepter = new LocalhostAccepter();
    
    private IConnectionAccepter localRefuseAccepter = new IConnectionAccepter() {
        public boolean accepts(InetAddress address) {
            return !localAccepter.accepts(address);
        }
    };
    
    public void testAcceptLocal() throws IOException {
        server.setConnectionAccepter(localAccepter);
        BinClient client = new BinClient("localhost",server.getPortNumber());
        client.start();
        assertEquals("a",client.send("echo",new Object[]{"a"}));
        client.shutdown();
    }
    
    public void testRefuseLocal() throws IOException {
        server.setConnectionAccepter(localRefuseAccepter);
        BinClient client = new BinClient("localhost",server.getPortNumber());
        try {
            client.start();
            client.send("echo",new Object[]{"a"});
        } catch (Exception e) {
            assertTrue(e instanceof Exception);
            //e.printStackTrace();
        }
        client.shutdown();
    }

    public void testMultiAcceptLocal() throws IOException {
        multiServer.setConnectionAccepter(localAccepter);
        BinClient client = new BinClient("localhost",multiServer.getPortNumber());
        client.start();
        assertEquals("a",client.send("echo",new Object[]{"a"}));
        client.shutdown();
    }
    
    public void testMultiRefuseLocal() throws IOException {
        multiServer.setConnectionAccepter(localRefuseAccepter);
        BinClient client = new BinClient("localhost",multiServer.getPortNumber());
        try {
            client.start();
            client.send("echo",new Object[]{"a"});
        } catch (Exception e) {
            assertTrue(e instanceof Exception);
        }
        client.shutdown();
    }

}
