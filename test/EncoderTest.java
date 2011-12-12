

import inou.net.Utils;
import inou.net.rpc.BinConstants;
import inou.net.rpc.BinDecoder;
import inou.net.rpc.BinEncoder;
import inou.net.rpc.IDecodeHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;


public class EncoderTest extends TestCase implements BinConstants {

	abstract class AbstractTestMethod {
		abstract void writeValue(OutputStream out) throws IOException;
		abstract void assertValue(TestDecoderHandler ret);
		boolean debug() {return false;}
	}

	public void primitiveTester(AbstractTestMethod tester) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		tester.writeValue(out);
		byte[] src = out.toByteArray();
		ByteArrayInputStream in = new ByteArrayInputStream(src);
		TestDecoderHandler ret = new TestDecoderHandler();
		int count = 0;
		while(count < out.size()) {
			int ss = BinDecoder.read(in,ret);
			if (tester.debug()) {
				byte[] aaa = new byte[ss];
				for(int i=0;i<ss;i++) {
					aaa[i] = src[count+i];
				}
				//System.out.println(Utils.dumpBinData(aaa));
				//System.out.println(ss+"/"+count+"/"+out.size());
			}
			count += ss;
		}
		try {
			tester.assertValue(ret);
		} catch (AssertionFailedError e) {
			System.err.println(Utils.dumpBinData(out.toByteArray()));
			System.err.println(ret.toString());
			throw e;
		}
	}

	public void testWriteIntegers() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BinEncoder.write(out,(byte)77);
		byte[] exp = {(byte)T_INTEGER1, (byte)77};
		assertEquals(exp,out.toByteArray());
		out.reset();

		BinEncoder.write(out,(short)0xff22);
		exp = new byte[]{(byte)T_INTEGER2, (byte)0x22,(byte)0xff};
		assertEquals(exp,out.toByteArray());
		out.reset();

		BinEncoder.write(out,(int)0x032244aa);
		exp = new byte[]{(byte)T_INTEGER4, (byte)0xaa,(byte)0x44,(byte)0x22,(byte)0x03,};
		assertEquals(exp,out.toByteArray());
		out.reset();

		BinEncoder.write(out,(long)0x032244aa00ff8811L);
		exp = new byte[]{(byte)T_INTEGER8, 
						 (byte)0x11,(byte)0x88,(byte)0xff,(byte)0x00,
						 (byte)0xaa,(byte)0x44,(byte)0x22,(byte)0x03,
		};
		assertEquals(exp,out.toByteArray());
		out.reset();
	}
    
    public void testHash() throws Exception {
        HashMap h = new HashMap();
        h.put("key1","string value");
        h.put(new Integer(15),new Integer(1234));
        h.put("array",new Object[]{"1","2",new Integer(34)});
        List list = new ArrayList();
        list.add("l1");list.add("l2");list.add("l3");
        h.put("list",list);
        HashMap m = new HashMap();
        m.put("ik1","val1");m.put("ik2","val2");m.put("ik3","val3");
        h.put("map",m);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinEncoder.writeObject(out,"header");
        BinEncoder.writeObject(out,h);
        BinEncoder.writeObject(out,new String[]{"footer"});
        byte[] src = out.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(src);
        
        TestDecoderHandler ret = new TestDecoderHandler();
        int count = 0;
        while(count < out.size()) {
            int ss = BinDecoder.read(in,ret);
            count += ss;
        }
        assertEquals("header",ret.sstr);
        assertEquals("footer",ret.astr[0]);

        Map ans = ret.map;
        assertEquals(h.get("key1"),ans.get("key1"));
        assertEquals(h.get(new Integer(15)),ans.get(new Integer(15)));
        assertTrue(compare((Object[])h.get("array"),(Object[])ans.get("array")));
        assertTrue(
                compare(((List)h.get("list")).toArray(),
                        (Object[])ans.get("list")
                ));
        assertEquals(h.get("map"),ans.get("map"));
    }

	public void testStream() {
		
	}
    
    private boolean compare(Object[] a1,Object[] a2) {
        if (a1 == null) throw new NullPointerException();
        if (a2 == null) throw new NullPointerException();
        if (a1.length != a2.length) throw new RuntimeException("a1="+a1.length+"  a2="+a2.length);
        boolean ret = true;
        for(int i=0;i<a1.length;i++) {
            if (a1[i].equals(a2[i])) continue;
            System.err.println(i+" : "+a1[i]+", "+a2[i]);
            ret = false;
        }
        return ret;
    }

	public void testReadIntegers() throws Exception {
		TestDecoderHandler hd = new TestDecoderHandler();

		byte[] exp = {(byte)T_INTEGER1, (byte)77};
		BinDecoder.read(new ByteArrayInputStream(exp),hd);
		assertEquals(77,hd.sbyte);

		exp = new byte[]{(byte)T_INTEGER2, (byte)0x22,(byte)0xff};
		BinDecoder.read(new ByteArrayInputStream(exp),hd);
		assertEquals((short)0xff22,hd.sshort);

		exp = new byte[]{(byte)T_INTEGER4, (byte)0xaa,(byte)0x44,(byte)0x22,(byte)0x03,};
		BinDecoder.read(new ByteArrayInputStream(exp),hd);
		assertEquals(0x032244aa,hd.sint);

		exp = new byte[]{(byte)T_INTEGER8, 
						 (byte)0x11,(byte)0x88,(byte)0xff,(byte)0x00,
						 (byte)0xaa,(byte)0x44,(byte)0x22,(byte)0x03,
		};
		BinDecoder.read(new ByteArrayInputStream(exp),hd);
		assertEquals(0x032244aa00ff8811L,hd.slong);
	}


	private void assertEquals(byte[] exp,byte[] sample) {
		if (sample == null) {
			fail("sample array is null!");
		}
		if (exp.length != sample.length) {
			fail("array length is not same.  expected:"+
				 exp.length+" => returned:"+sample.length+"\n"+
					 "\nexp: "+Utils.dumpBinData(exp)+
					 "\nret: "+Utils.dumpBinData(sample));
		}
		for(int i=0;i<exp.length;i++) {
			if (exp[i] != sample[i]) {
				fail("different bytes: pos="+i+"  length="+exp.length+
					 "\nexp: "+Utils.dumpBinData(exp)+
					 "\nret: "+Utils.dumpBinData(sample));
			}
		}
	}

	public void testPrimitive() throws Exception {
		final String aaa = "Hello world! 日本語";
		primitiveTester(new AbstractTestMethod() {
				void writeValue(OutputStream out) throws IOException {
					BinEncoder.write(out,false);
					BinEncoder.write(out,true);
					BinEncoder.write(out,(byte)-1);
					BinEncoder.write(out,(short)-2);
					BinEncoder.write(out,(int)-3);
					BinEncoder.write(out,(long)-4);
					BinEncoder.write(out,(float)-5.2);
					BinEncoder.write(out,(double)-6.2);
					BinEncoder.write(out,aaa);
					BinEncoder.write(out,new BigDecimal("-12"));
				}
				void assertValue(TestDecoderHandler ret) {
					assertTrue(ret.sbool);
					assertEquals(-1,ret.sbyte);
					assertEquals(-2,ret.sshort);
					assertEquals(-3,ret.sint);
					assertEquals(-4l,ret.slong);
					assertTrue(-5.2f == ret.sfloat);
					assertTrue(-6.2d == ret.sdouble);
					assertEquals(aaa,ret.sstr);
					assertEquals(new BigDecimal(-12),ret.sdecimal);
				}
			});
	}

	public void testWriteIntArray() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BinEncoder.writeArray(out,new byte[]{(byte)77,(byte)0xff});
		byte[] exp = {(byte)T_ARRAY,(byte)T_INTEGER1, (byte)2,0,0,0, (byte)77,(byte)0xff};
		assertEquals(exp,out.toByteArray());
		out.reset();

		BinEncoder.writeArray(out,new short[]{(short)0xff22,(short)0x1234});
		exp = new byte[]{(byte)T_ARRAY,(byte)T_INTEGER2,2,0,0,0,
						 (byte)0x22,(byte)0xff,(byte)0x34,(byte)0x12,};
		assertEquals(exp,out.toByteArray());
		out.reset();
	}

	public void testNullList() throws Exception {
		final List list = new LinkedList();
		list.add("Array");
		list.add(null);
		list.add(new Double(10));
		list.add(Boolean.TRUE);
		primitiveTester(new AbstractTestMethod() {
				boolean debug() { return false; }
				void writeValue(OutputStream out) throws IOException {
					BinEncoder.write(out,(int)1);
					BinEncoder.writeObject(out,null);
					BinEncoder.writeObject(out,Boolean.FALSE);
					BinEncoder.writeList(out,list.toArray());
					BinEncoder.writeArray(out,new String[]{"P1",null,"P2",});
					BinEncoder.writeArray(out,new boolean[]{false,true,true,false,});
				}
				void assertValue(TestDecoderHandler ret) {
					assertEquals(1,ret.sint);
					assertTrue(ret.snull);
					assertFalse(ret.sbool);

					List rlist = new ArrayList(Arrays.asList(ret.list));
					assertEquals("Array",rlist.get(0));
					assertNull(rlist.get(1));
					assertEquals(new Double(10),rlist.get(2));
					assertEquals(Boolean.TRUE,rlist.get(3));

					assertEquals("P1",ret.astr[0]);
					assertNull(ret.astr[1]);
					assertEquals("P2",ret.astr[2]);

					assertTrue(!ret.abool[0]);
					assertTrue(ret.abool[1]);
					assertTrue(ret.abool[2]);
					assertTrue(!ret.abool[3]);
				}
			});
	}


	public void testReadIntArray() throws Exception {
		TestDecoderHandler hd = new TestDecoderHandler();

		byte[] exp = {(byte)T_ARRAY,(byte)T_INTEGER1, (byte)2,0,0,0, (byte)77,(byte)0xff};
		int sz = BinDecoder.read(new ByteArrayInputStream(exp),hd);
		assertEquals(sz,exp.length);
		assertEquals(77,hd.abyte[0]);
		assertEquals((byte)0xff,hd.abyte[1]);

		exp = new byte[]{(byte)T_ARRAY,(byte)T_INTEGER2,2,0,0,0,
						 (byte)0x22,(byte)0xff,(byte)0x34,(byte)0x12,};
		sz = BinDecoder.read(new ByteArrayInputStream(exp),hd);
		assertEquals(sz,exp.length);
		assertEquals((short)0xff22,hd.ashort[0]);
		assertEquals((short)0x1234,hd.ashort[1]);
	}

	public void testArray() throws Exception {
		final String[] aaa = {"Hello","World!","日本語"};
		primitiveTester(new AbstractTestMethod() {
				void writeValue(OutputStream out) throws IOException {
					BinEncoder.writeArray(out,new byte[]{(byte)-1,(byte)1});
					BinEncoder.writeArray(out,new short[]{(short)-2,(short)2});
					BinEncoder.writeArray(out,new int[]{-3,3});
					BinEncoder.writeArray(out,new long[]{-4,4});
					BinEncoder.writeArray(out,new float[]{-5.2f,5.2f});
					BinEncoder.writeArray(out,new double[]{-6.2,6.2});
					BinEncoder.writeArray(out,aaa);
					BinEncoder.writeArray(out,new BigDecimal[]{new BigDecimal("-12"),new BigDecimal("123456789.01234567890")});
				}
				void assertValue(TestDecoderHandler ret) {
					assertEquals(-1,ret.abyte[0]);
					assertEquals( 1,ret.abyte[1]);
					assertEquals(-2,ret.ashort[0]);
					assertEquals( 2,ret.ashort[1]);
					assertEquals(-3,ret.aint[0]);
					assertEquals( 3,ret.aint[1]);
					assertEquals(-4l,ret.along[0]);
					assertEquals( 4l,ret.along[1]);
					assertTrue(-5.2f == ret.afloat[0]);
					assertTrue( 5.2f == ret.afloat[1]);
					assertTrue(-6.2d == ret.adouble[0]);
					assertTrue( 6.2d == ret.adouble[1]);
					assertEquals(aaa[0],ret.astr[0]);
					assertEquals(aaa[1],ret.astr[1]);
					assertEquals(aaa[2],ret.astr[2]);
					assertEquals(new BigDecimal("-12"),ret.adecimal[0]);
					assertEquals(new BigDecimal("123456789.01234567890"),ret.adecimal[1]);
				}
			});
	}

	public void testList() throws Exception {
		final List list = new ArrayList();
		list.add(new Byte((byte)-1));
		list.add(new Short((short)-2));
		list.add(new Integer(-3));
		list.add(new Long(-4));
		list.add(new Float(-5.3f));
		list.add(new Double(-6.3));
		list.add(new BigDecimal("123456789.123456789"));
		list.add("OK?");
		list.add(new int[]{1,-2,-3});
		list.add(new String[]{"Hello","World","日本語"});
		List ilist = new ArrayList();
		ilist.add(new Integer(1));
		ilist.add(new Integer(2));
		list.add(ilist);
		primitiveTester(new AbstractTestMethod() {
				void writeValue(OutputStream out) throws IOException {
					BinEncoder.writeList(out,list.toArray());
				}
				void assertValue(TestDecoderHandler ret) {
					List rlist = new ArrayList(Arrays.asList(ret.list));
					assertEquals(list.get(0),rlist.get(0));
					assertEquals(list.get(2),rlist.get(2));
					assertEquals(list.get(4),rlist.get(4));
					assertEquals(list.get(6),rlist.get(6));
					int[] ia = (int[])rlist.get(8);
					int[] il = (int[])list.get(8);
					assertEquals(il[0],ia[0]);
					assertEquals(il[1],ia[1]);
					assertEquals(il[2],ia[2]);
					String[] ss = (String[])rlist.get(9);
					String[] sl = (String[])list.get(9);
					assertEquals(sl[0],ss[0]);
					assertEquals(sl[1],ss[1]);
					assertEquals(sl[2],ss[2]);
					Object[] alist = (Object[])rlist.get(10);
					assertEquals(new Integer(1),alist[0]);
					assertEquals(new Integer(2),alist[1]);
				}
			});
	}

	private class TestDecoderHandler implements IDecodeHandler {

		boolean snull = false;
		boolean sbool;
		byte sbyte;
		short sshort;
		int sint;
		long slong;
		float sfloat;
		double sdouble;
		String sstr;
		BigDecimal sdecimal;
		
		boolean[] abool;
		byte[] abyte;
		short[] ashort;
		int[] aint;
		long[] along;
		float[] afloat;
		double[] adouble;
		String[] astr;
		BigDecimal[] adecimal;
		Object[] anull;

		Object[] list;
        Map map;

        Object obj;

		public String toString() {
			StringBuffer sb = new StringBuffer("=== Values ======").append("\n");
			sb.append("bool:"+sbool).append("\n");
			sb.append("byte:"+sbyte).append("\n");
			sb.append("short:"+sshort).append("\n");
			sb.append("int:"+sint).append("\n");
			sb.append("long:"+slong).append("\n");
			sb.append("float:"+sfloat).append("\n");
			sb.append("double:"+sdouble).append("\n");
			sb.append("String:"+sstr).append("\n");
			sb.append("decimal:"+sdecimal).append("\n");

			sb.append("bool[]:"+abool).append("\n");
			sb.append("byte[]:"+abyte).append("\n");
			sb.append("short[]:"+ashort).append("\n");
			sb.append("int[]:"+aint).append("\n");
			sb.append("long[]:"+along).append("\n");
			sb.append("float[]:"+afloat).append("\n");
			sb.append("double[]:"+adouble).append("\n");
			sb.append("String[]:"+astr).append("\n");
			sb.append("decimal[]:"+adecimal).append("\n");
			sb.append("null[]:"+adecimal).append("\n");

			if (list != null) {
				sb.append("LIST:\n");
				for(int i=0;i<list.length;i++) {
					sb.append("  ").append(i).append(":").append(list[i]).append("\n");
				}
			}
            if (map != null) {
                sb.append("HASH:\n{ ");
                Iterator it = map.keySet().iterator();
                while(it.hasNext()) {
                    Object key = it.next();
                    Object val = map.get(key);
                    sb.append(key).append(": ").append(val).append(", ");
                }
                sb.append("}\n");
            }
			sb.append("object:"+obj.getClass().getName()+" ["+obj.toString()+"]").append("\n");

			return sb.toString();
		}

		public void readNull() {
			snull = true;
		}
		public void readBool(boolean a) {
			sbool = a;
		}
		public void readByte(byte a) {
			sbyte = a;
		}
		public void readShort(short a) {
			sshort = a;
		}
		public void readInt(int a) {
			sint = a;
		}
		public void readLong(long a) {
			slong = a;
		}
		public void readFloat(float a) {
			sfloat = a;
		}
		public void readDouble(double a) {
			sdouble = a;
		}
		public void readString(String a) {
			sstr = a;
		}
		public void readDecimal(BigDecimal a) {
			sdecimal = a;
		}

		public void readArray(boolean[] a) {
			abool = a;
		}
		public void readArray(byte[] a) {
			abyte = a;
		}
		public void readArray(short[] a) {
			ashort = a;
		}
		public void readArray(int[] a) {
			aint = a;
		}
		public void readArray(long[] a) {
			along = a;
		}
		public void readArray(float[] a) {
			afloat = a;
		}
		public void readArray(double[] a) {
			adouble = a;
		}
		public void readArray(String[] a) {
			astr = a;
		}
		public void readArray(BigDecimal[] a) {
			adecimal = a;
		}
		public void readArray(Object[] a) {
			anull = a;
		}

		public void readList(Object[] a) {
			list = a;
		}
        public void readHash(Map a) {
            map = a;
        }
		public void readObject(Object a) {
			this.obj = a;
		}
	}
	
}
