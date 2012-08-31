package inou.net.rpc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;

import inou.net.InvocationPool;
import org.apache.log4j.Logger;

public class MultiBinServer implements BinConstants,IServiceManager {

    private Logger monitor = Logger.getLogger(this.getClass());
    private ServerSocket serverSocket;
    private ServiceManager handlerManager = new ServiceManager();
    private int soTimeout = 300;
    private InvocationPool invocationPool;
    
    private List connectionList = new LinkedList();
    private IConnectionAccepter connectionAccepter;

    private boolean shutdownFlag = false;
    private Object shutdownFlagLock = new Object();

    public MultiBinServer() throws IOException {
        this(0);
    }

    public MultiBinServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    /**
       for SSL Server Socket
     */
    public MultiBinServer(ServerSocket s) {
        this.serverSocket = s;
    }

	public void restart() {
		invocationPool.init();
	}
    
    public void setInvocationPool(InvocationPool pool) {
        invocationPool = pool;
    }
    
    public void setSoTimeout(int s) {
        if (s > 0) {
            soTimeout = s;
        }
    }
    
    public IConnectionAccepter getConnectionAccepter() {
        return connectionAccepter;
    }

    public void setConnectionAccepter(IConnectionAccepter ca) {
        connectionAccepter = ca;
    }
    
    public int getPortNumber() {
        return serverSocket.getLocalPort();
    }
    
    public void addHandler(String name,IMessageHandler h) {
        handlerManager.addHandler(name,h);
    }

    public void removeHandler(String name) {
        handlerManager.removeHandler(name);
    }

    public void shutdown() {
        monitor.debug("MultiBinServer: shutdown signal arrived.");
        synchronized (shutdownFlagLock) {
            shutdownFlag = true;
            try {
                shutdownFlagLock.wait(soTimeout+20);
            } catch (InterruptedException e) {
                monitor.warn("Interruption during the shutdown process.",e);
            }
        }
        while(true) {
            ConnectionDealer cd = null;
            synchronized (connectionList) {
                if (connectionList.isEmpty()) break;
                monitor.info("MultBinServer: There are "+connectionList.size()+" live connections.");
                cd = (ConnectionDealer)connectionList.get(0);
            }
            cd.shutdown();
            monitor.info("MultBinServer: a live connection is killed.");
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            monitor.warn("Exception during socket closing.",e);
        }
        invocationPool.dispose();
        monitor.debug("MultiBinServer: shutdown process finished.");
    }

    private Socket waitingForClientConnection(ServerSocket ss) throws IOException {
        if (invocationPool == null) {
            invocationPool = new InvocationPool(4,100,getClass().getName()+"|POOL");
        }
        ss.setSoTimeout(soTimeout);
        synchronized (shutdownFlagLock) {
            if (shutdownFlag) {
                throw new SocketTimeoutException("Socket is shutdown.");
            }
        }
        while(true) {
            try {
                Socket socket = ss.accept();
                if (connectionAccepter != null){
                    SocketAddress sa = socket.getRemoteSocketAddress();
                    if (sa instanceof InetSocketAddress && 
                        connectionAccepter.accepts( ((InetSocketAddress)sa).getAddress()) ) {
                        monitor.debug("MultiBinServer: connection is granted. ["+sa.toString()+"]");
                    } else {
                        monitor.warn("MultiBinServer: connection refused. ["+sa.toString()+"]");
                        socket.close();
                        continue;
                    }
                }
                return socket;
            } catch (SocketTimeoutException e) {
                synchronized (shutdownFlagLock) {
                    if (shutdownFlag) {
                        throw e;
                    }
                }
                continue;
            }
        }
    }

    public ICommunicator getClientConnection() throws IOException {
		return getClientConnection(null);
	}

    public ICommunicator getClientConnection(ServiceManager _customServiceManager) throws IOException {
		if (_customServiceManager == null) {
			_customServiceManager = new ServiceManager(handlerManager);
		} else {
			_customServiceManager.setParentManager(handlerManager);
		}
        final ServiceManager customServiceManager = _customServiceManager;
        Socket socket = waitingForClientConnection(serverSocket);
        ConnectionDealer connectionDealer = new ConnectionDealer(socket,customServiceManager);
        synchronized (connectionList) {
            connectionList.add(connectionDealer);
        }
        final MessageServer messageServer = connectionDealer.getMessageServer();
        return new ICommunicator() {
            public void addHandler(String name, IMessageHandler h) {
                customServiceManager.addHandler(name,h);
            }
            public void removeHandler(String name) {
                customServiceManager.removeHandler(name);
            }
            public void setStreamGenerator(IStreamGenerator isg) {
                customServiceManager.setStreamGenerator(isg);
            }
            public void start() {
            }
            public void shutdown() {
                messageServer.shutdown();
            }
            public boolean isConnected() {
                return messageServer.isConnected();
            }
            public InetAddress getRemoteAddress() {
                return messageServer.getSocket().getInetAddress();
            }
            public Object send(String name, Object[] args) throws IOException {
                return messageServer.send(name,args);
            }
        };
    }
    
    class ConnectionDealer implements Runnable {
        private Socket socket;
        private MessageServer messageServer;
        ConnectionDealer(Socket s,ServiceManager sm) throws IOException {
            socket = s;
            if (socket == null) {
                throw new NullPointerException("socket object is null!");
            }
            messageServer = new MessageServer("SV",sm,invocationPool);
            messageServer.setSocket(socket);
        }
        public void run() {
            try {
                monitor.info("ConnectionDealer: Working thread is started.");
                messageServer.blockWorkingThread(); 
                monitor.info("ConnectionDealer: Disconnected.");
            } catch (Throwable e) {
                monitor.warn(e.getMessage(),e);
            } finally {
                shutdown();
                synchronized (connectionList) {
                    connectionList.remove(this);
                }
                monitor.debug("ConnectionDealer: Thread finished.");
            }
        }
        public MessageServer getMessageServer() {
            Thread thread = new Thread(this,"CDealer");
            thread.start();
            return messageServer;
        }
        public void shutdown() {
            messageServer.shutdown();
            try {
                socket.close();
            } catch (IOException e) {
            }
            monitor.debug("ConnectionDealer: Connection finished.");
        }
    }
    
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        final MultiBinServer server = new MultiBinServer(port);

        BinServer.setupEchoServer(server);

        final boolean[] stopper = {false};
        server.addHandler("exit",new IMessageHandler() {
                public Object send(Object[] arg) throws Exception {
                    synchronized(stopper) {
                        stopper[0] = true;
                        stopper.notifyAll();
                    }
                    return null;
                }
            });
        System.out.println("Server started on port["+port+"]. Send [exit] message to shutdown this server.");
        new Thread(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        server.getClientConnection();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        },"TestEchoServer").start();
        
        while(true) {
            synchronized(stopper) {
                if (stopper[0]) {
                    Thread.sleep(300);
                    server.shutdown();
                    Thread.sleep(300);
                    System.exit(0);
                }
                stopper.wait();
            }
        }

    }
}
