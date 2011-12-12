package inou.net.rpc;


import java.awt.event.InvocationEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import inou.net.InvocationPool;
import org.apache.log4j.Logger;

/**
 * Single Point-to-Point RPC server.
 */
public class BinServer implements BinConstants,ICommunicator {

	private static Logger monitor = Logger.getLogger(BinServer.class);
	private ServerSocket serverSocket;
	private ServiceManager handlerManager = new ServiceManager();
	private MessageServer messageServer;
    private InvocationPool invocationPool = new InvocationPool(2,100,"BS|POOL");
    
    private IConnectionAccepter connectionAccepter;
    private int connectionTimeoutSec = 2; 

	private Thread socketThread;
	private boolean shutdownFlag = false;

	public BinServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		messageServer = new MessageServer("SV",handlerManager,invocationPool);
	}

    public void setDebugFile(File path) throws IOException {
        if (isConnected()) {
            throw new RuntimeException("Debug file should be set before connection starting.");
        }
        messageServer.setDebugOutput(new FileOutputStream(path));
    }

	public BinServer() throws IOException {
		this(0);
	}

	public int getPortNumber() {
		return serverSocket.getLocalPort();
	}
    
    public boolean isConnected() {
        return messageServer.isConnected();
    }

    public InetAddress getRemoteAddress() {
        return messageServer.getSocket().getInetAddress();
    }

    public IConnectionAccepter getConnectionAccepter() {
        return connectionAccepter;
    }

    public void setConnectionAccepter(IConnectionAccepter ca) {
        connectionAccepter = ca;
    }
    
    public void setConnectionTimeoutSec(int s) {
        connectionTimeoutSec = s;
    }

	public void setStreamGenerator(IStreamGenerator isg) {
		handlerManager.setStreamGenerator(isg);
	}

    public void start() {
		invocationPool.init();
		socketThread = new Thread(acceptor);
		socketThread.setName("BSV|socket");
		socketThread.start();
	}

	public void shutdown() {
		monitor.debug("BinServer: shutdown signal arrived.");
		shutdownFlag = true;
		messageServer.shutdown();
        invocationPool.dispose();
		while(true) {
			if (socketThread == null) {
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				monitor.warn(e.getMessage(),e);
				return;
			}
		}
		monitor.debug("BinServer: shutdown completed.");
	}

	private Runnable acceptor = new Runnable() {
			public void run() {
				try {
					while(!shutdownFlag) {
						Socket s = null;
						try {
							monitor.info("BinServer: waiting for client's connection...");
                            s = waitingForClientConnection(serverSocket);
                            if (connectionAccepter != null){
                                SocketAddress sa = s.getRemoteSocketAddress();
                                if (sa instanceof InetSocketAddress && 
                                    connectionAccepter.accepts( ((InetSocketAddress)sa).getAddress()) ) {
                                    monitor.debug("BinServer: connection is granted. ["+sa.toString()+"]");
                                } else {
                                    monitor.warn("BinServer: connection refused. ["+sa.toString()+"]");
                                    s.close();
                                    continue;
                                }
                            }
							messageServer.setSocket(s);
							monitor.info("BinServer: connection established.");
							messageServer.blockWorkingThread();	
							monitor.info("BinServer: disconnected.");
						} catch (SocketTimeoutException e) {
							continue;
						} catch (IOException e) {
							monitor.warn(e.getMessage(),e);
							return;
						}
					}
				} finally {
					monitor.debug("BinServer: Acceptor thread finished.");
					socketThread = null;
					shutdownFlag = false;
				}
			}
		};
	
	private Socket waitingForClientConnection(ServerSocket ss) throws IOException {
		ss.setSoTimeout(300);
		while(true) {
			try {
				return ss.accept();
			} catch (SocketTimeoutException e) {
				if (shutdownFlag) {
					throw e;
				}
				continue;
			}
		}
	}

	public void addHandler(String name,IMessageHandler h) {
		handlerManager.addHandler(name,h);
	}

	public Object send(String name,Object[] args) throws IOException {
		return messageServer.send(name,args);
	}

	public void removeHandler(String name) {
		handlerManager.removeHandler(name);
	}

	//===== echo server

	private static String array2str(Object array) {
		StringBuffer sb = new StringBuffer("[ ");
		int num = Array.getLength(array);
		for(int i=0;i<num;i++) {
			Object obj = Array.get(array,i);
			if (obj == null) {
				sb.append("null");
			} else if (obj.getClass().isArray()) {
				sb.append( array2str(obj) );
			} else {
				sb.append(obj.toString());
			}
			sb.append( i < (num-1) ? ", " : "");
		}
		sb.append(" ]");
		return sb.toString();
	}

	public static void main(String[] args) throws Exception {
		int port = (args.length == 0) ? 9999 : Integer.parseInt(args[0]);
		BinServer server = new BinServer(port);
        //server.setDebugFile(new File("packet-server.dat"));

        setupEchoServer(server);

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
		server.start();
		monitor.info("Server started on port["+port+"]. Send [exit] message to shutdown this server.");
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
    
    static void setupEchoServer(IServiceManager serviceManager) {
            //primitive checker

            serviceManager.addHandler("echo_boolean",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        return new Boolean(((Boolean)arg[0]).booleanValue());
                    }
                });
            serviceManager.addHandler("echo_int1",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        return new Byte((byte)(((Number)arg[0]).byteValue()));
                    }
                });
            serviceManager.addHandler("echo_int2",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        return new Short((short)(((Number)arg[0]).shortValue()));
                    }
                });
            serviceManager.addHandler("echo_int4",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        return new Integer(((Number)arg[0]).intValue());
                    }
                });
            serviceManager.addHandler("echo_int8",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        return new Long(((Number)arg[0]).longValue());
                    }
                });
            serviceManager.addHandler("echo_string",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        String s = (String)arg[0];
                        return s;
                    }
                });
            serviceManager.addHandler("echo_null",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        return arg[0];
                    }
                });
            serviceManager.addHandler("echo_decimal",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        BigDecimal s = null;
                        if (arg[0] instanceof BigDecimal) {
                            s = (BigDecimal)arg[0];
                        } else {
                            s = new BigDecimal(arg[0].toString());
                        }
                        return s;
                    }
                });
            serviceManager.addHandler("echo_float",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        return new Float(((Number)arg[0]).floatValue());
                    }
                });
            serviceManager.addHandler("echo_double",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(arg[0]);
                        return new Double(((Number)arg[0]).doubleValue());
                    }
                });

            //array checker

            serviceManager.addHandler("echo_boolean_array",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(array2str(arg[0]));
                        boolean[] ba = (boolean[])(arg[0]);
                        monitor.info("boolean_array: ");
                        return ba;
                    }
                });
            serviceManager.addHandler("echo_int1_array",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(array2str(arg[0]));
                        byte[] ba = (byte[])arg[0];
                        return ba;
                    }
                });
            serviceManager.addHandler("echo_int2_array",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(array2str(arg[0]));
                        short[] ba = (short[])arg[0];
                        return ba;
                    }
                });
            serviceManager.addHandler("echo_int4_array",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(array2str(arg[0]));
                        int[] ba = (int[])arg[0];
                        return ba;
                    }
                });
            serviceManager.addHandler("echo_int8_array",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(array2str(arg[0]));
                        long[] ba = (long[])arg[0];
                        return ba;
                    }
                });
            serviceManager.addHandler("echo_string_array",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(array2str(arg[0]));
                        String[] ba = (String[])arg[0];
                        return ba;
                    }
                });
            serviceManager.addHandler("echo_decimal_array",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(array2str(arg[0]));
                        BigDecimal[] ba = (BigDecimal[])arg[0];
                        return ba;
                    }
                });
            serviceManager.addHandler("echo_float_array",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(array2str(arg[0]));
                        float[] ba = (float[])arg[0];
                        return ba;
                    }
                });
            serviceManager.addHandler("echo_double_array",new IMessageHandler() {
                    public Object send(Object[] arg) throws Exception {
                        monitor.info(array2str(arg[0]));
                        double[] ba = (double[])arg[0];
                        return ba;
                    }
                });

            //list and hash checker

            serviceManager.addHandler("echo_list",new IMessageHandler() {
                    public Object send(Object[] a) throws Exception {
                        Object[] arg = (Object[])a[0];
                        List list = new ArrayList();
                        for (int i=0;i<arg.length;i++) {
                            list.add(arg[i]);
                        }
                        return list;
                    }
                });
            serviceManager.addHandler("echo_hash",new IMessageHandler() {
                    public Object send(Object[] a) throws Exception {
                        HashMap arg = (HashMap)a[0];
                        HashMap ret = new HashMap();
                        Iterator it = arg.keySet().iterator();
                        while(it.hasNext()) {
                            Object k = it.next();
                            ret.put(k,arg.get(k));
                        }
                        return ret;
                    }
                });
    }

}
