package inou.net.rpc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;

import inou.net.InvocationPool;
import org.apache.log4j.Logger;


public class BinClient implements BinConstants,ICommunicator{

	private Logger monitor = Logger.getLogger(this.getClass());
	private String host;
	private int port;
    
    private boolean reconnect = false;
	private int reconnectWaitTime = 2000;

    private Exception exceptionInAccepter;
    private int startupTimeoutMSec = 2000; 

	private ServiceManager handlerManager = new ServiceManager();
	private MessageServer messageServer;
    private InvocationPool invocationPool;

	private Thread socketThread;
	private boolean shutdownFlag = false;

    public BinClient(String host,int port) {
        this.host = host;
        this.port = port;
        invocationPool = new InvocationPool(2,100,"CL|POOL");
        messageServer = new MessageServer("CL",handlerManager,invocationPool);
    }

    public void setDebugFile(File path) throws IOException {
        if (isConnected()) {
            throw new RuntimeException("Debug file should be set before connection starting.");
        }
        messageServer.setDebugOutput(new FileOutputStream(path));
    }

	public void setStreamGenerator(IStreamGenerator isg) {
		handlerManager.setStreamGenerator(isg);
	}

    public void setReconnect(boolean b) {
        reconnect = b;
    }
    
    public boolean isReconnect() {
        return reconnect;
    }

	/**
	 * waiting time for auto re-connect to the server.
	 *
	 * @param a miliseconds
	 */
	public void setReconnectWaitTime(int a) {
		reconnectWaitTime = a;
	}

	public int getReconnectWaitTime() {
		return reconnectWaitTime;
	}

    public boolean isConnected() {
        return messageServer.isConnected();
    }

    public InetAddress getRemoteAddress() {
        return messageServer.getSocket().getInetAddress();
    }

    public void setConnectionTimeoutMiliSec(int s) {
        if (s > 0) {
            startupTimeoutMSec = s;
        }
    }

    public synchronized void start() {
		shutdownFlag = false;
        if (socketThread != null) {
            return;
        }
		invocationPool.init();
		socketThread = new Thread(connector);
		socketThread.setName("BCL|socket");
		socketThread.start();
        int delta = startupTimeoutMSec / 20;
        for(int i=0;i<20;i++) {
            try {
                Thread.sleep(delta);
            } catch (InterruptedException e) {
                monitor.warn(e.getClass().getName()+" : "+e.getMessage(),e);
            }
            if (messageServer.isConnected()) {
                return;
            } else if (exceptionInAccepter != null) {
                throw new RuntimeException(exceptionInAccepter);
            }
        }
        throw new RuntimeException("Startup timeout.");
	}

	public synchronized void shutdown() {
		monitor.debug("BinClient: shutdown signal arrived.");
		shutdownFlag = true;
		messageServer.shutdown();
		while(true) {
			if (socketThread == null) {
				break;
			}
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				monitor.warn(e.getMessage(),e);
				return;
			}
		}
		monitor.debug("BinClient: shutdown completed.");
		shutdownFlag = false;
	}

	private Runnable connector = new Runnable() {
			public void run() {
				try {
					while(!shutdownFlag) {
						try {
							monitor.info("BinClient: connecting...");
							Socket socket = new Socket(host,port);
							monitor.info("BinClient: connection established.");
							messageServer.setSocket(socket);
							messageServer.blockWorkingThread();
							monitor.info("BinClient: disconnected.");
						} catch (IOException e) {
							monitor.warn(e.getMessage(),e);
                            exceptionInAccepter = e;
						}
                        if (!reconnect) break;
						try {
							Thread.sleep(reconnectWaitTime);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
                } catch (Exception e) {
                    if (exceptionInAccepter == null) {
                        exceptionInAccepter = e;
                    }
				} finally {
					monitor.debug("BinClient: Connector thread finished.");
                    invocationPool.dispose();
                    invocationPool = null;
					socketThread = null;
				}
			}
		};

	public void addHandler(String name,IMessageHandler h) {
		handlerManager.addHandler(name,h);
	}

	public Object send(String name,Object[] args) throws IOException {
		return messageServer.send(name,args);
	}
	
	public void removeHandler(String name) {
		handlerManager.removeHandler(name);
	}

}
