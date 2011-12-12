import inou.net.rpc.BinClient;
import inou.net.rpc.BinServer;
import inou.net.rpc.FileStreamGenerator;
import inou.net.rpc.IMessageHandler;
import inou.net.rpc.StreamHolderClass;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import junit.framework.TestCase;


public class StreamTest extends TestCase {

	private BinServer server;

	public void setUp() throws IOException {
		server = new BinServer();
		server.start();
	}

	private boolean check(byte[] b1,byte[] b2) {
		if (b1 == null || b2 == null || b1.length != b2.length) {
			return false;
		}
		for(int i=0;i<b1.length;i++) {
			if (b1[i] != b2[i]) return false;
		}
		return true;
	}
	
	public void testStreamUpload() throws Exception {
		final byte[] sample = {(byte)0x11,(byte)0x88,(byte)0xff,(byte)0x00,
							   (byte)0xaa,(byte)0x44,(byte)0x22,(byte)0x03,};
		server.addHandler("stream",new IMessageHandler() {
				public Object send(Object[] args) {
					byte[] ret = (byte[])args[0];
					return new Boolean(check(sample,ret));
				}
			});
		BinClient client = new BinClient("localhost",server.getPortNumber());
		client.start();
		Boolean b = (Boolean)client.send
			("stream",new Object[]{new StreamHolderClass
								   (sample.length,new ByteArrayInputStream(sample))});
		assertTrue("stream upload",b.booleanValue());
		client.shutdown();
	}

	public void testStreamDownload() throws Exception {
		final byte[] sample = {(byte)0x11,(byte)0x88,(byte)0xff,(byte)0x00,
							   (byte)0xaa,(byte)0x44,(byte)0x22,(byte)0x03,};
		server.setStreamGenerator(new FileStreamGenerator());
		server.addHandler("stream",new IMessageHandler() {
				public Object send(Object[] args) {
					File f = (File)args[0];
					try {
						byte[] array = new byte[(int)(f.length())];
						InputStream in = new FileInputStream(f);
						in.read(array);
						assertEquals(sample.length,(int)f.length());
						assertTrue(check(sample,array));
						in.close();
						return new Boolean(check(sample,array));
					} catch (Exception e) {
						throw new RuntimeException(e);
					} finally {
						assertTrue(f.delete());
					}
				}
			});
		BinClient client = new BinClient("localhost",server.getPortNumber());
		client.start();
		Boolean b = (Boolean)client.send("stream",new Object[]{sample});
		assertTrue("stream download",b.booleanValue());
		client.shutdown();
	}

	public void testStreamRoundTrip() throws Exception {
		final byte[] sample = {(byte)0x11,(byte)0x88,(byte)0xff,(byte)0x00,
							   (byte)0xaa,(byte)0x44,(byte)0x22,(byte)0x03,};
		server.setStreamGenerator(new FileStreamGenerator());
		server.addHandler("stream",new IMessageHandler() {
				public Object send(Object[] args) {
					File f = (File)args[0];
					try {
						byte[] array = new byte[(int)(f.length())];
						InputStream in = new FileInputStream(f);
						in.read(array);
						assertEquals(sample.length,(int)f.length());
						assertTrue(check(sample,array));
						in.close();
						return new StreamHolderClass
							(array.length,new ByteArrayInputStream(array));
					} catch (Exception e) {
						throw new RuntimeException(e);
					} finally {
						assertTrue(f.delete());
					}
				}
			});
		BinClient client = new BinClient("localhost",server.getPortNumber());
		client.setStreamGenerator(new FileStreamGenerator());
		client.start();
		Object obj = client.send
			("stream",new Object[]{new StreamHolderClass
								   (sample.length,new ByteArrayInputStream(sample))});

		assertEquals(File.class,obj.getClass());

		File ret = (File)obj;
		byte[] array = new byte[(int)(ret.length())];
		InputStream in = new FileInputStream(ret);
		in.read(array);
		assertEquals(sample.length,(int)ret.length());
		assertTrue(check(sample,array));
		in.close();
		assertTrue(ret.delete());
		
		client.shutdown();
	}

	public void testVeryLargeData() throws Exception {
		final long[] checkSum = {0};
		final int size = 1024 * 1024 * 1; //change 
		server.setStreamGenerator(new FileStreamGenerator(new File(".")));
		server.addHandler("stream_large",new IMessageHandler() {
				public Object send(Object[] args) {
					File f = (File)args[0];
					try {
						int num = (int)f.length();
						long sum = 0;
						InputStream in = new BufferedInputStream(new FileInputStream(f));
						for(int i=0;i<num;i++) {
							sum += in.read();
						}
						assertEquals(checkSum[0],sum);
						in.close();
						return null;
					} catch (Exception e) {
						throw new RuntimeException(e);
					} finally {
						assertTrue(f.delete());
					}
				}
			});
		InputStream in = new InputStream() {
				int count = 0;
				public int read() {
					if (count == size) return -1;
					int ret = (int)(Math.random()*256);
					checkSum[0] += ret;
					count++;
					return ret;
				}
			};
		BinClient client = new BinClient("localhost",server.getPortNumber());
		client.start();
		client.send("stream_large",new Object[]{new StreamHolderClass(size,in)});
		client.shutdown();
	}

	public void tearDown() {
		server.shutdown();
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(StreamTest.class);
	}

}
