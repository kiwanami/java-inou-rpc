import inou.net.rpc.BinClient;
import inou.net.rpc.BinServer;
import inou.net.rpc.ICommunicator;
import inou.net.rpc.IMessageHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;


public class ClientServerTest extends TestCase {

	private BinServer server;

	public void setUp() throws IOException {
		server = new BinServer();
		server.start();
	}

	public void testClient2ServerMessage() throws Exception {
		final String[] ret = {null};
		server.addHandler("c2s",new IMessageHandler() {
				public Object send(Object[] arg) throws Exception {
					ret[0] = (String)arg[0];
					return "OK";
				}
			});
		int port = server.getPortNumber();
		BinClient client = new BinClient("localhost",port);
		client.start();
		String result = (String)client.send("c2s",new Object[]{"Hello!"});
		assertEquals("Hello!",ret[0]);
		assertEquals("OK",result);
		server.removeHandler("s2c");
		client.shutdown();
	}

	public void testServer2ClientMessage() throws Exception {
		final String[] ret = {null};
		int port = server.getPortNumber();
		BinClient client = new BinClient("localhost",port);
		client.start();
		client.addHandler("s2c",new IMessageHandler() {
				public Object send(Object[] arg) throws Exception {
					ret[0] = (String)arg[0];
					return "オーケー!!";
				}
			});
		String result = (String)server.send("s2c",new Object[]{"日本語!"});
		assertEquals("日本語!",ret[0]);
		assertEquals("オーケー!!",result);
		client.removeHandler("s2c");
		client.shutdown();
	}

	public void testArrayArgs() throws Exception {
		final int[] arg1 = {1,2,3,4,5,6,7,8,9,10};
		final int[] arg2 = {2,4,6};
		int port = server.getPortNumber();
		BinClient client = new BinClient("localhost",port);
		client.start();
		client.addHandler("array",new IMessageHandler() {
				public Object send(Object[] args) throws Exception {
					int[] a1 = (int[])args[0];
					int[] a2 = (int[])args[1];
					int[] ret = new int[a1.length*a2.length];
					for(int i=0;i<a2.length;i++) {
						for(int j=0;j<a1.length;j++) {
							ret[i*a1.length+j] = a1[j]*a2[i];
						}
					}
					return ret;
				}
			});
		int[] ret = (int[])server.send("array",new Object[]{arg1,arg2});
		for(int i=0;i<arg2.length;i++) {
			for(int j=0;j<arg1.length;j++) {
				assertEquals(arg1[j]*arg2[i],ret[i*arg1.length+j]);
			}
		}
		client.removeHandler("array");
		client.shutdown();
	}

	public void testArrayArgs2() throws Exception {
		final String[] arg1 = {null,"before",null,"AFTER"};
		int port = server.getPortNumber();
		BinClient client = new BinClient("localhost",port);
		client.start();
		client.addHandler("array2",new IMessageHandler() {
				public Object send(Object[] args) throws Exception {
					String[] ret = (String[])args[0];
					return ret;
				}
			});
		String[] ret = (String[])server.send("array2",new Object[]{arg1});
		for(int j=0;j<arg1.length;j++) {
			assertEquals(arg1[j],ret[j]);
		}
		client.removeHandler("array");
		client.shutdown();
	}

	public void testMultiThread() throws Exception {
		int port = server.getPortNumber();
		BinClient client = new BinClient("localhost",port);
		IMessageHandler int_echo = new IMessageHandler() {
				public Object send(Object[] args) {
					Integer i = (Integer)args[0];
					return new Integer(i.intValue()*2);
				}
			};
		IMessageHandler double_echo = new IMessageHandler() {
				public Object send(Object[] args) {
					Double i = (Double)args[0];
					return new Double(i.doubleValue()*2);
				}
			};
		IMessageHandler long_echo = new IMessageHandler() {
				public Object send(Object[] args) {
					Long i = (Long)args[0];
					return new Long(i.longValue()*2);
				}
			};
		IMessageHandler str_echo = new IMessageHandler() {
				public Object send(Object[] args) {
					String i = (String)args[0];
					String[] s = (String[])args[1];
					return i+s[0]+s[1];
				}
			};
		server.addHandler("int_echo",int_echo);
		server.addHandler("double_echo",double_echo);
		server.addHandler("long_echo",long_echo);
		server.addHandler("str_echo",str_echo);
		client.addHandler("int_echo",int_echo);
		client.addHandler("double_echo",double_echo);
		client.addHandler("long_echo",long_echo);
		client.addHandler("str_echo",str_echo);
		client.start();

		int tnum = 3;
		int anum = 20;
		List list = new LinkedList();
		for(int i=0;i<tnum;i++) {
			new Thread(new TesterThread_int(client,anum,list)).start();
			new Thread(new TesterThread_double(client,anum,list)).start();
			new Thread(new TesterThread_long(client,anum,list)).start();
			new Thread(new TesterThread_array(client,anum,list)).start();

			new Thread(new TesterThread_int(server,anum,list)).start();
			new Thread(new TesterThread_double(server,anum,list)).start();
			new Thread(new TesterThread_long(server,anum,list)).start();
			new Thread(new TesterThread_array(server,anum,list)).start();
		}
		while(list.size() < (tnum*8)) {
			synchronized(list) {
				list.wait();
			}
		}

        assertEquals(client.getWaitingSessionNumber(),0);
        assertEquals(client.getSendingQueueNumber(),0);

		client.shutdown();
	}

	class TesterThread_int implements Runnable {
		private ICommunicator client;
		private int times;
		private List list;
		TesterThread_int(ICommunicator c,int t,List l) {
			client = c;
			times = t;
			list = l;
		}
		public void run() {
			for(int i=0; i<times; i++) {
				try {
					Integer in = new Integer(i);
					Integer ret = (Integer)client.send("int_echo",new Object[]{in});
					assertEquals(in.intValue()*2, ret.intValue());
					Thread.sleep((long)(Math.random()*40));
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
			synchronized(list) {
				list.add(this);
				list.notifyAll();
			}
		}
	}

	class TesterThread_double implements Runnable {
		private ICommunicator client;
		private int times;
		private List list;
		TesterThread_double(ICommunicator c,int t,List l) {
			client = c;
			times = t;
			list = l;
		}
		public void run() {
			for(int i=0; i<times; i++) {
				try {
					Double in = new Double(Math.random()*100);
					Double ret = (Double)client.send("double_echo",new Object[]{in});
					assertTrue(in.doubleValue()*2 == ret.doubleValue());
					Thread.sleep((long)(Math.random()*40));
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
			synchronized(list) {
				list.add(this);
				list.notifyAll();
			}
		}
	}

	class TesterThread_long implements Runnable {
		private ICommunicator client;
		private int times;
		private List list;
		TesterThread_long(ICommunicator c,int t,List l) {
			client = c;
			times = t;
			list = l;
		}
		public void run() {
			for(int i=0; i<times; i++) {
				try {
					Long in = new Long((int)(Math.random()*100000000));
					Long ret = (Long)client.send("long_echo",new Object[]{in});
					assertTrue(in.longValue()*2 == ret.longValue());
					Thread.sleep((long)(Math.random()*40));
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
			synchronized(list) {
				list.add(this);
				list.notifyAll();
			}
		}
	}

	class TesterThread_array implements Runnable {
		private ICommunicator client;
		private int times;
		private List list;
		TesterThread_array(ICommunicator c,int t,List l) {
			client = c;
			times = t;
			list = l;
		}
		public void run() {
			for(int i=0; i<times; i++) {
				try {
					String in1 = "aa";
					String in2 = "bb";
					String in3 = "cc";
					String ret = (String)client.send("str_echo",new Object[]{in1,new String[]{in2,in3}});
					assertEquals("aabbcc",ret);
					Thread.sleep((long)(Math.random()*40));
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
			synchronized(list) {
				list.add(this);
				list.notifyAll();
			}
		}
	}


	public void testError() throws Exception {
		int port = server.getPortNumber();
		BinClient client = new BinClient("localhost",port);
		client.start();

		server.addHandler("err",new IMessageHandler() {
				public Object send(Object[] arg) throws Exception {
					throw new RuntimeException("ApplicationError!");
				}
			});
		try {
			client.send("err",null); //application error
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().indexOf("ApplicationError!")>=0);
		}

		try {
			client.send("err1",null); //wrong method
		} catch (IOException e) {
			assertTrue(e.getMessage().indexOf("NoSuch")>=0);
		}

		server.addHandler("err",new IMessageHandler() {
				public Object send(Object[] arg) throws Exception {
					throw new InternalError("VMError?");
				}
			});
		try {
			client.send("err",null);
		} catch (InternalError e) {
			assertTrue(e.getMessage().indexOf("VMError")>=0);
		}

        assertEquals(client.getWaitingSessionNumber(),0);
        assertEquals(client.getSendingQueueNumber(),0);

		server.removeHandler("err");
		client.shutdown();
	}

	public void testSerializationError() throws Exception {
		int port = server.getPortNumber();
		BinClient client = new BinClient("localhost",port);
		client.start();

		server.addHandler("err",new IMessageHandler() {
				public Object send(Object[] arg) throws Exception {
					throw new RuntimeException("ApplicationError!");
				}
			});

		try {
			client.send("err", new Object[]{this}); //serialization error
		} catch (IOException e) {
			assertTrue(e.getMessage().indexOf("BinStreamException")>=0);
		}

        assertEquals(client.getWaitingSessionNumber(),0);
        assertEquals(client.getSendingQueueNumber(),0);

		server.removeHandler("err");
		client.shutdown();
	}

	public void testSendNull() throws Exception {
		int port = server.getPortNumber();
		final BinClient client = new BinClient("localhost",port);
		server.addHandler("nuller",new IMessageHandler() {
				public Object send(Object[] args) throws Exception {
					assertEquals("1",args[0]);
					assertNull(args[1]);
					return null;
				}
			});
		client.start();
		Object obj = client.send("nuller",new Object[]{"1",null});
		assertNull(obj);
		server.removeHandler("nuller");
		client.shutdown();
	}

    public void testSendList() throws Exception {
        int port = server.getPortNumber();
        final BinClient client = new BinClient("localhost",port);
        server.addHandler("list",new IMessageHandler() {
                public Object send(Object[] args) throws Exception {
                    assertTrue(args[0] instanceof Object[]);
                    Object[] list = (Object[])args[0];
                    assertEquals(2,list.length);
                    return null;
                }
            });
        client.start();
        List a = new ArrayList();
        a.add(new Integer(3)); a.add("hoge");
        Object obj = client.send("list",new Object[]{a});
        assertNull(obj);
        server.removeHandler("list");
        client.shutdown();
    }

    public void testSendHash() throws Exception {
        int port = server.getPortNumber();
        final BinClient client = new BinClient("localhost",port);
        server.addHandler("hash",new IMessageHandler() {
                public Object send(Object[] args) throws Exception {
                    Map hash = (Map)args[0]; 
                    assertEquals("1",hash.get("first"));
                    assertNull(hash.get("invalid"));
                    return hash;
                }
            });
        client.start();
        HashMap h = new HashMap();
        h.put("first","1");
        Object obj = client.send("hash",new Object[]{h});
        assertEquals(h.get("first"),((Map)obj).get("first"));
        server.removeHandler("hash");
        client.shutdown();
    }

	public void testReconnect() throws Exception {
		final String[] ret = {null};
		server.addHandler("c2s",new IMessageHandler() {
				public Object send(Object[] arg) throws Exception {
					ret[0] = (String)arg[0];
					return "OK";
				}
			});
		int port = server.getPortNumber();
		BinClient client = new BinClient("localhost",port);
		client.setReconnect(true);
		client.setReconnectWaitTime(100);
		client.start();
		
		String result = (String)client.send("c2s",new Object[]{"Hello!"});
		assertEquals("Hello!",ret[0]);
		assertEquals("OK",result);

		server.shutdown();
		Thread.sleep(1000);
		server.start();
		Thread.sleep(1000);

		result = (String)client.send("c2s",new Object[]{"Hello!"});
		assertEquals("Hello!",ret[0]);
		assertEquals("OK",result);

		server.shutdown();
		Thread.sleep(1000);
		server.start();
		Thread.sleep(1000);

		result = (String)client.send("c2s",new Object[]{"Hello!"});
		assertEquals("Hello!",ret[0]);
		assertEquals("OK",result);

		server.removeHandler("s2c");
		client.shutdown();
	}

    public void testInetAddress() {
		int port = server.getPortNumber();
		BinClient client = new BinClient("localhost",port);
		client.start();

        String clientAddr = server.getRemoteAddress().getHostAddress();
        assertEquals("client IP", "127.0.0.1", clientAddr);
        String serverAddr = client.getRemoteAddress().getHostAddress();
        assertEquals("server IP", "127.0.0.1", serverAddr);

        client.shutdown();
    }

	public void tearDown() {
		server.shutdown();
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(ClientServerTest.class);
	}

}
