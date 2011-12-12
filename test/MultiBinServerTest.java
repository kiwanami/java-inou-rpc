import inou.net.rpc.BinClient;
import inou.net.rpc.ICommunicator;
import inou.net.rpc.IMessageHandler;
import inou.net.rpc.MultiBinServer;
import inou.net.rpc.ServiceManager;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import junit.framework.TestCase;

public class MultiBinServerTest extends TestCase {

    private MultiBinServer serverGenerator;
    private Logger monitor = Logger.getLogger(this.getClass());
    
    protected void setUp() throws Exception {
        serverGenerator = new MultiBinServer();
    }
    
    /**
     * とりあえず、Server側とClient側の両方から通信を行ってみるテスト
     */
    public void testNormalConnection() throws Exception {
        serverGenerator.addHandler("echo",new IMessageHandler() {
            public Object send(Object[] args) throws Exception {
                return "ECHO:"+args[0];
            }
        });
        BinClient client = new BinClient("localhost",serverGenerator.getPortNumber());
        client.addHandler("echo",new IMessageHandler() {
            public Object send(Object[] args) throws Exception {
                return "ECHO:"+args[0];
            }
        });
        
        //　serverGenerator.getClientConnection() がブロックするので
        // 別スレッドで接続を待って、接続が来たら server[] に代入する。 
        // lock は接続完了まで待つための排他オブジェクト。
        final ICommunicator[] server = new ICommunicator[1];
        final Object lock = new Object();
        new Thread(new Runnable() {
            public void run() {
                try {
                    server[0] = serverGenerator.getClientConnection();
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        client.start();
        while(true) {
            synchronized (lock) {
                if (server[0] != null) break;
                lock.wait();
            }
        }
        assertEquals("ECHO:first",client.send("echo",new Object[]{"first"}));
        assertEquals("ECHO:second",server[0].send("echo",new Object[]{"second"}));
        client.shutdown();
    }

    /**
     * 一度に多数接続して、一つずつ実行
     * @throws Exception
     */
    public void testMultiConnection() throws Exception {
        monitor.debug("########### test MultiConnection ");
        IMessageHandler handler = new IMessageHandler() {
            public Object send(Object[] args) throws Exception {
                Integer i1 = (Integer)args[0];
                Integer i2 = (Integer)args[1];
                return new Integer(i1.intValue()+i2.intValue());
            }
        };
        serverGenerator.addHandler("add",handler);
        final BinClient[] clients = new BinClient[5];
        for(int i=0;i<clients.length;i++) {
            clients[i] = new BinClient("localhost",serverGenerator.getPortNumber());
            clients[i].addHandler("add",handler);
        }
        
        //　serverGenerator.getClientConnection() がブロックするので
        // 別スレッドで接続を待って、ひとつずつ接続をつないでいく。
        final List servers = new ArrayList();
        final boolean[] ready = {false};
        final Exception[] exceptionHolder = new Exception[1];
        new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        synchronized (ready) {
                            ready[0] = true;
                            ready.notifyAll();
                        }
                        servers.add(serverGenerator.getClientConnection());
                        monitor.debug("##### Servers: ["+servers.size()+"/"+clients.length+"]");
                        synchronized (ready) {
                            try {
                                if (ready[0]) {
                                    ready.wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (servers.size() == clients.length) break;
                    }
                } catch (IOException e) {
                    exceptionHolder[0] = e;
                }
                synchronized (servers) {
                    servers.notifyAll();
                }
            }
        }).start();
        for(int i=0;i<clients.length;i++) {
            synchronized (ready) {
                if (!ready[0]) {
                    ready.wait();
                }
            }
            clients[i].start();
            synchronized (ready) {
                ready[0] = false;
                ready.notify();
            }
            monitor.debug("##### Connected: ["+i+"/"+clients.length+"]");
            if (exceptionHolder[0] != null){
                monitor.warn("#### Exception : "+exceptionHolder[0].getClass().getName()+" : "+exceptionHolder[0].getMessage());
                throw new RuntimeException(exceptionHolder[0]);
            }
        }
        while(true) {
            synchronized (servers) {
                if (servers.size() == clients.length) {
                    break;
                }
                servers.wait();
            }
        }
        //全接続を確認
        monitor.debug("##### Connected: "+servers.size());
        for(int i=0;i<clients.length;i++) {
            monitor.debug("########### start ["+i+"/"+clients.length+"]");
            BinClient client = clients[i];
            ICommunicator server = (ICommunicator)servers.get(i);
            final String mname = "custom"+i;
            server.addHandler(mname,new IMessageHandler(){
                public Object send(Object[] args) throws Exception {
                    return mname;
                }});
            Integer i1 = new Integer((int)(Math.random()*500));
            Integer i2 = new Integer((int)(Math.random()*500));
            Integer ans = new Integer(i1.intValue()+i2.intValue());
            assertEquals(ans,client.send("add",new Object[]{i1,i2}));
            assertEquals(ans,server.send("add",new Object[]{i2,i1}));
            assertEquals(mname,client.send(mname,null));
            client.shutdown();
        }
    }
    
    class NestServerHandler implements IMessageHandler {
        private ICommunicator client;
        NestServerHandler(ICommunicator c) {
            client = c;
        }
        public Object send(Object[] args) throws Exception {
            Integer i1 = (Integer)args[0];
            Integer i2 = (Integer)args[1];
            long waitTime = 500+(long)(Math.random()*500);
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            client.send("back_result",new Object[]{new Integer(i1.intValue()+i2.intValue())} );
            return null;
        }
    }
    
    class NestClientHandler implements IMessageHandler {
        private ICommunicator client;
        NestClientHandler(ICommunicator c) {
            client = c;
        }
        public Object send(Object[] args) throws Exception {
            client.shutdown();
            return null;
        }
    }
    
    /**
     * 一度に多数接続して、ネストされた通信から切断
     * @throws Exception
     */
    public void testMultiConnection2() throws Exception {
        monitor.debug("########### test MultiConnection Async ");
        final BinClient[] clients = new BinClient[20];
        for(int i=0;i<clients.length;i++) {
            clients[i] = new BinClient("localhost",serverGenerator.getPortNumber());
            clients[i].addHandler("back_result",new NestClientHandler(clients[i]));
        }
        
        //　serverGenerator.getClientConnection() がブロックするので
        // 別スレッドで接続を待って、ひとつずつ接続をつないでいく。
        final List servers = new ArrayList();
        final boolean[] ready = {false};
        final Exception[] exceptionHolder = new Exception[1];
        new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        synchronized (ready) {
                            ready[0] = true;
                            ready.notifyAll();
                        }
                        servers.add(serverGenerator.getClientConnection());
                        monitor.debug("##### Servers: ["+servers.size()+"/"+clients.length+"]");
                        synchronized (ready) {
                            try {
                                if (ready[0]) {
                                    ready.wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (servers.size() == clients.length) break;
                    }
                } catch (IOException e) {
                    exceptionHolder[0] = e;
                }
                synchronized (servers) {
                    servers.notifyAll();
                }
            }
        }).start();
        for(int i=0;i<clients.length;i++) {
            synchronized (ready) {
                if (!ready[0]) {
                    ready.wait();
                }
            }
            clients[i].start();
            synchronized (ready) {
                ready[0] = false;
                ready.notify();
            }
            monitor.debug("##### Connected: ["+i+"/"+clients.length+"]");
            if (exceptionHolder[0] != null){
                monitor.warn("#### Exception : "+exceptionHolder[0].getClass().getName()+" : "+exceptionHolder[0].getMessage());
                throw new RuntimeException(exceptionHolder[0]);
            }
        }
        while(true) {
            synchronized (servers) {
                if (servers.size() == clients.length) {
                    break;
                }
                servers.wait();
            }
        }
        //全接続を確認
        monitor.debug("##### Connected: "+servers.size());
        for(int i=0;i<clients.length;i++) {
            final int id = i;
            final ICommunicator server = (ICommunicator)servers.get(i);
            final BinClient client = clients[i];
            server.addHandler("add",new NestServerHandler(server));
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    monitor.debug("########### start ["+id+"/"+clients.length+"]");
                    Integer i1 = new Integer((int)(Math.random()*500));
                    Integer i2 = new Integer((int)(Math.random()*500));
                    try {
                        client.send("add",new Object[]{i2,i1});
                    } catch (Throwable e) {
                        assertTrue("EOFException check", e instanceof EOFException);
                        //e.printStackTrace();
                    } finally {
                        synchronized (servers) {
                            servers.remove(server);
                            servers.notifyAll();
                        }
                        monitor.debug("########### end   ["+id+"/"+clients.length+"]");
                    }
                }
            });
            thread.start();
        }
        while(true) {
            synchronized (servers) {
                if (servers.size() == 0) {
                    break;
                }
                servers.wait();
            }
        }
    }

	/**
	   複数の処理を一度に付加するテスト。
	   さらに、接続毎に独自処理を追加したときに、デフォルトの処理を上書きできるかも確認。
	*/
	public void testCommonHandlerSet() throws Exception {
		serverGenerator.addHandler("echo1",new IMessageHandler() {
				public Object send(Object[] args) throws Exception {
					return args[0];
				}
			});
        BinClient client1 = new BinClient("localhost",serverGenerator.getPortNumber());
        BinClient client2 = new BinClient("localhost",serverGenerator.getPortNumber());
        final ICommunicator[] server = new ICommunicator[2];
        final Object lock = new Object();
        new Thread(new Runnable() {
            public void run() {
                try {
					ServiceManager sm = new ServiceManager();
					sm.addHandler("echo1",new IMessageHandler() {
							public Object send(Object[] args) throws Exception {
								return args[0]+"1";
							}
						});
					sm.addHandler("echo2",new IMessageHandler() {
							public Object send(Object[] args) throws Exception {
								return args[0].toString()+"2";
							}
						});
                    server[0] = serverGenerator.getClientConnection(sm);
                    server[1] = serverGenerator.getClientConnection();
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        client1.start();
        client2.start();
        while(true) {
            synchronized (lock) {
                if (server[1] != null) break;
                lock.wait();
            }
        }
        assertEquals("echo1",client1.send("echo1",new Object[]{"echo"}));
        assertEquals("echo2",client1.send("echo2",new Object[]{"echo"}));
        assertEquals("echo",client2.send("echo1",new Object[]{"echo"}));
        client1.shutdown();
	}

    protected void tearDown() throws Exception {
        serverGenerator.shutdown();
    }

}
