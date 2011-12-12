

import inou.net.rpc.BinClient;

import java.io.File;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class EchoTest extends TestCase {

	private int port = 9999;

	private Object[] primitiveSource = {
		"boolean",new Boolean(true),
		"boolean",new Boolean(false),
		"int1",new Byte("10"),
		"int1",new Byte("-10"),
		"int2",new Short("10"),
		"int2",new Short("-10"),
		"int4",new Integer(10),
		"int4",new Integer(-10),
		"int8",new Long(10),
		"int8",new Long(-10),
		"decimal",new BigDecimal("3141592653589795635987653123478987345234"),
		"decimal",new BigDecimal("-3141592653589795635987653123478987345234"),
		"float",new Float(10f),
		"float",new Float(-10f),
		"double",new Double(10),
		"double",new Double(-10),
		"string","#$%ETYUFGVGGA",
	};

	private boolean equalWithString(Object a,Object b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return (a.toString().equals(b.toString()));
	}

	private boolean equalsList(List a,List b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		if (a.size() != b.size()) throw new RuntimeException("Different list size. a:"+a.size()+" -> "+b.size());
		for(int i=0;i<a.size();i++) {
			Object la = a.get(i);
			Object lb = b.get(i);
			if (la instanceof Object[]) {
				la = Arrays.asList((Object[])la);
			}
			if (lb instanceof Object[]) {
				lb = Arrays.asList((Object[])lb);
			}
			if (la instanceof List) {
				assertTrue(equalsList((List)la,(List)lb));
			} else if (!equalWithString(la,lb)) {
				throw new RuntimeException("Different item.  ["+i+"] a:"+a.get(i)+" -> b:"+b.get(i));
			}
		}
		return true;
	}

	private BinClient client;

	public void testPrimitive() throws Exception {
		for(int i=0;i<primitiveSource.length;i+=2) {
			String name = (String)primitiveSource[i];
			Object arg = primitiveSource[i+1];
			Object ret = client.send("echo_"+name,new Object[]{arg});
			assertTrue(equalWithString(arg,ret));
		}
	}

	public void testList() throws Exception {
		List arg = new ArrayList();
		arg.add("1234");
		arg.add(new Integer(10));
		arg.add(null);
		arg.add(new Double(-1234));
		Object ret = client.send("echo_list",new Object[]{arg});
		assertTrue(ret instanceof Object[]);
		assertTrue(equalsList(arg,Arrays.asList((Object[])ret)));
	}

	public void testNestedList() throws Exception {
		List alist = new ArrayList();
		alist.add(new Integer(1));
		alist.add(new Integer(10));
		alist.add("list");
		HashMap amap = new HashMap();
		amap.put("123","a message");
		amap.put("234","OK");
		amap.put("456","Test");

		List arg = new ArrayList();
		arg.add("1234");
		arg.add(new Integer(10));
		arg.add(null);
		arg.add(new Double(-1234));
		arg.add(alist);
		arg.add(amap);

		Object ret = client.send("echo_list",new Object[]{arg});
		assertTrue(ret instanceof Object[]);
		assertTrue(equalsList(arg,Arrays.asList((Object[])ret)));
	}

	private boolean equalsHash(Map a,Map b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		if (a.size() != b.size()) throw new RuntimeException("Different hash size. a:"+a.size()+" -> "+b.size());
		for(Iterator it = a.keySet().iterator();it.hasNext();) {
			Object key = it.next();
			Object va = a.get(key);
			Object vb = b.get(key);
			if (!equalWithString(va,vb)) {
				throw new RuntimeException("Different item.  ["+key+"] a:"+va+" -> b:"+vb);
			}
		}
		return true;
	}

	public void testHash() throws Exception {
		HashMap arg = new HashMap();
		arg.put("key1","");
		arg.put("key2","");
		arg.put("num",new Integer(123));
		arg.put("bignum",new BigDecimal("1234879872639487623412309817643"));
		Object ret = client.send("echo_hash",new Object[]{arg});
		assertTrue(ret instanceof HashMap);
		assertTrue(equalsHash(arg,(HashMap)ret));
	}

	private Object[] arraySource = {
		"boolean",new boolean[]{true,false,true,false},
		"int1",new byte[]{-1,-2,1,2,0},
		"int2",new short[]{-1,-2,0,12,3},
		"int4",new int[]{-1,-2,0,2,3},
		"int8",new long[]{-1,-2,0,2,3},
		"decimal",new BigDecimal[]{
                new BigDecimal("3141592653589795635987653123478987345234"),
                new BigDecimal("-3141592653589795635987653123478987345234")},
		"float",new float[]{-1,-2,0,2,3},
		"double",new double[]{-1,-2,0,2,3},
		"string",new String[]{"a "," ds"," 2132"},
	};

	private boolean equalsArray(Object a,Object b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		int al = Array.getLength(a);
		int bl = Array.getLength(b);
		if (al != bl) throw new RuntimeException("Different array size. a:"+al+" -> "+bl);
		for(int i=0;i<al;i++) {
			Object la = Array.get(a,i);
			Object lb = Array.get(b,i);
			if (!equalWithString(la,lb)) {
				throw new RuntimeException("Different item.  ["+i+"] a:"+la+" -> b:"+lb);
			}
		}
		return true;
	}

	public void testArray() throws Exception {
		for(int i=0;i<arraySource.length;i+=2) {
			String name = (String)arraySource[i];
			Object arg = arraySource[i+1];
			Object ret = client.send("echo_"+name+"_array",new Object[]{arg});
			assertTrue(equalsArray(arg,ret));
		}
	}

	public void setUp() throws Exception {
		client = new BinClient("localhost",port);
        //client.setDebugFile(new File("packet-client.dat"));
		client.start();
	}

	public void tearDown() {
		client.shutdown();
	}

	public static void main(String[] args) throws Exception {
        /*
        EchoTest et = new EchoTest();
        et.setUp();
        et.testArray();
        et.tearDown();
        */
		junit.textui.TestRunner.run(EchoTest.class);
	}

}
