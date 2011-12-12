package inou.net.rpc;


import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.InputStream;


public class BinEncoder implements BinConstants {

	public final static int write(OutputStream out, boolean a) throws IOException {
		out.write(a ? T_BOOLEAN_TRUE : T_BOOLEAN_FALSE);
		return 2;
	}

	public final static int write(OutputStream out, byte a) throws IOException {
		out.write(T_INTEGER1);
		out.write(a);
		return 2;
	}

	public final static int write(OutputStream out, short a) throws IOException {
		out.write(T_INTEGER2);
		return 1 + writeInt2(out,a);
	}

	public final static int write(OutputStream out, int a) throws IOException {
		out.write(T_INTEGER4);
		return 1 + writeInt4(out,a);
	}

	private final static int writeInt4(OutputStream out,int a) throws IOException {
		out.write(a & 0xff);
		out.write((a>>8) & 0xff);
		out.write((a>>16) & 0xff);
		out.write((a>>24) & 0xff);
		return 4;
	}

	private final static int writeInt2(OutputStream out,int a) throws IOException {
		out.write(a & 0xff);
		out.write((a>>8) & 0xff);
		return 2;
	}

	public final static int write(OutputStream out, long a) throws IOException {
		out.write(T_INTEGER8);
		return 1 + writeLong(out,a);
	}

	private final static int l2b(long a) {
		return (int)(a & 0xff);
	}

	private final static int writeLong(OutputStream out,long a) throws IOException {
		out.write(l2b(a));
		out.write(l2b(a>>8));
		out.write(l2b(a>>16));
		out.write(l2b(a>>24));
		out.write(l2b(a>>32));
		out.write(l2b(a>>40));
		out.write(l2b(a>>48));
		out.write(l2b(a>>56));
		return 8;
	}

	public final static int write(OutputStream out, float a) throws IOException {
		out.write(T_FLOAT);
		return 1 + writeInt4(out, Float.floatToIntBits(a));
	}

	public final static int write(OutputStream out, double a) throws IOException {
		out.write(T_DOUBLE);
		return 1 + writeLong(out, Double.doubleToLongBits(a));
	}

	private final static int writeString(OutputStream out,String a) throws IOException {
		if (a == null) {
			writeInt4(out,-1);
			return 4;
		}
		byte[] ret = null;
		try {
			ret = a.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			//the exception will be never throwen.
			e.printStackTrace();
			ret = a.getBytes();
		}
		writeInt4(out,ret.length);
		out.write(ret);
		return 4+ret.length;
	}

	public final static int write(OutputStream out,BigDecimal a) throws IOException {
		if (a == null) return writeNull(out);
		out.write(T_DECIMAL);
		return 1 + writeString(out,a.toString());
	}
	
	public final static int write(OutputStream out,String a) throws IOException {
		if (a == null) return writeNull(out);
		out.write(T_STRING);
		return 1 + writeString(out,a);
	}

	public final static int writeObject(OutputStream out,Object obj) throws IOException {
		if (obj == null) {
			return writeNull(out);
		} else if (obj instanceof Boolean) {
			return write(out,Boolean.TRUE.equals(obj));
		} else if (obj instanceof String) {
			return write(out,(String)obj);
		} else if (obj instanceof Number) {
			return writeNumber(out,(Number)obj);
		} else if (obj instanceof int[]) {
			return writeArray(out,(int[])obj);
		} else if (obj instanceof double[]) {
			return writeArray(out,(double[])obj);
		} else if (obj instanceof byte[]) {
			return writeArray(out,(byte[])obj);
		} else if (obj instanceof String[]) {
			return writeArray(out,(String[])obj);
		} else if (obj instanceof long[]) {
			return writeArray(out,(long[])obj);
		} else if (obj instanceof short[]) {
			return writeArray(out,(short[])obj);
		} else if (obj instanceof float[]) {
			return writeArray(out,(float[])obj);
		} else if (obj instanceof boolean[]) {
			return writeArray(out,(boolean[])obj);
		} else if (obj instanceof BigDecimal[]) {
			return writeArray(out,(BigDecimal[])obj);
		} else if (obj instanceof Object[]) {
			return writeList(out,(Object[])obj);
		} else if (obj instanceof List) {
			return writeList(out,(List)obj);
        } else if (obj instanceof Map) {
            return writeHash(out,(Map)obj);
		} else if (obj instanceof IStreamHolder) {
			return writeStream(out,(IStreamHolder)obj);
		} else {
			throw new BinStreamException("Can not write the object: "+obj.getClass().getName());
		}
	}

	public final static int writeNumber(OutputStream out,Number obj) throws IOException {
		if (obj == null) {
			return writeNull(out);
		} else if (obj instanceof Integer) {
			return write(out,obj.intValue());
 		} else if (obj instanceof Double) {
			return write(out,obj.doubleValue());
 		} else if (obj instanceof Byte) {
			return write(out,obj.byteValue());
 		} else if (obj instanceof Long) {
			return write(out,obj.longValue());
 		} else if (obj instanceof Short) {
			return write(out,obj.shortValue());
 		} else if (obj instanceof Float) {
			return write(out,obj.floatValue());
 		} else if (obj instanceof BigDecimal) {
			return write(out,(BigDecimal)obj);
		}
		throw new BinStreamException("Can not write the object: "+obj.getClass().getName()+" : "+obj.toString());
	}

	//==== array

	public final static int writeNull(OutputStream out) throws IOException {
		out.write(T_NULL);
		return 1;
	}

	public final static int writeArray(OutputStream out,boolean[] obj) throws IOException {
		out.write(T_ARRAY);
		out.write(T_BOOLEAN_TRUE);
		writeInt4(out,obj.length);
		for(int i=0;i<obj.length;i++) {
			out.write(obj[i] ? T_BOOLEAN_TRUE : T_BOOLEAN_FALSE);
		}
		return 6+obj.length;
	}

	public final static int writeArray(OutputStream out,byte[] obj) throws IOException {
		out.write(T_ARRAY);
		out.write(T_INTEGER1);
		writeInt4(out,obj.length);
		out.write(obj);
		return 6+obj.length;
	}

	public final static int writeStream(OutputStream out,IStreamHolder is) throws IOException {
		try {
			return writeStream(out,is.getSize(),is.getStream());
		} catch (IOException e) {
			is.onException(e);
			throw e;
		} catch (RuntimeException e) {
			is.onException(e);
			throw e;
		} finally {
			is.onFinished();
		}
	}

	public final static int writeStream(OutputStream out,int size,InputStream in) throws IOException {
		out.write(T_ARRAY);
		out.write(T_INTEGER1);
		writeInt4(out,size);
		byte[] buf = new byte[Math.min(size,1024*8)];
		int count = 0;
		while(true) {
			int len = in.read(buf);
			if (len < 0) break;
			count += len;
			out.write(buf,0,len);
		}
		if (size != count) {
			throw new BinStreamException("The given stream length ["+size+"] is different from real length ["+count+"].");
		}
		return 6+size;
	}

	public final static int writeArray(OutputStream out,short[] obj) throws IOException {
		out.write(T_ARRAY);
		out.write(T_INTEGER2);
		writeInt4(out,obj.length);
		for(int i=0;i<obj.length;i++) {
			writeInt2(out,obj[i]);
		}
		return 6+obj.length*2;
	}

	public final static int writeArray(OutputStream out,int[] obj) throws IOException {
		out.write(T_ARRAY);
		out.write(T_INTEGER4);
		writeInt4(out,obj.length);
		for(int i=0;i<obj.length;i++) {
			writeInt4(out,obj[i]);
		}
		return 6+obj.length*4;
	}

	public final static int writeArray(OutputStream out,long[] obj) throws IOException {
		out.write(T_ARRAY);
		out.write(T_INTEGER8);
		writeInt4(out,obj.length);
		for(int i=0;i<obj.length;i++) {
			writeLong(out,obj[i]);
		}
		return 6+obj.length*8;
	}

	public final static int writeArray(OutputStream out,float[] obj) throws IOException {
		out.write(T_ARRAY);
		out.write(T_FLOAT);
		writeInt4(out,obj.length);
		for(int i=0;i<obj.length;i++) {
			writeInt4(out,Float.floatToIntBits(obj[i]));
		}
		return 6+obj.length*4;
	}

	public final static int writeArray(OutputStream out,double[] obj) throws IOException {
		out.write(T_ARRAY);
		out.write(T_DOUBLE);
		writeInt4(out,obj.length);
		for(int i=0;i<obj.length;i++) {
			writeLong(out,Double.doubleToLongBits(obj[i]));
		}
		return 6+obj.length*8;
	}

	public final static int writeArray(OutputStream out,String[] obj) throws IOException {
		out.write(T_ARRAY);
		out.write(T_STRING);
		writeInt4(out,obj.length);
		int size = 6;
		for(int i=0;i<obj.length;i++) {
			size += writeString(out,obj[i]);
		}
		return size;
	}

	public final static int writeArray(OutputStream out,BigDecimal[] obj) throws IOException {
		out.write(T_ARRAY);
		out.write(T_DECIMAL);
		writeInt4(out,obj.length);
		int size = 6;
		for(int i=0;i<obj.length;i++) {
			size += writeString(out,obj[i].toString());
		}
		return size;
	}

    public final static int writeList(OutputStream out,List list) throws IOException {
        return writeList(out,list.toArray());
    }

    public final static int writeList(OutputStream out,Object[] obj) throws IOException {
		out.write(T_LIST);
		writeInt4(out,obj.length);
		int size = 5;
		for(int i=0;i<obj.length;i++) {
			Object a = obj[i];
			if (a == null) {
				size += writeNull(out);
			} else if (a instanceof Number) {
				size += writeNumber(out,(Number)a);
			} else if (a instanceof String) {
				size += write(out,(String)a);
			} else if (a instanceof int[]) {
				size += writeArray(out,(int[])a);
			} else if (a instanceof double[]) {
				size += writeArray(out,(double[])a);
			} else if (a instanceof byte[]) {
				size += writeArray(out,(byte[])a);
			} else if (a instanceof String[]) {
				size += writeArray(out,(String[])a);
            } else if (a instanceof long[]) {
                size += writeArray(out,(long[])a);
			} else if (a instanceof short[]) {
				size += writeArray(out,(short[])a);
			} else if (a instanceof float[]) {
				size += writeArray(out,(float[])a);
			} else if (a instanceof BigDecimal[]) {
				size += writeArray(out,(BigDecimal[])a);
			} else if (a instanceof Boolean) {
				size += write(out,Boolean.TRUE.equals(a));
			} else if (a instanceof boolean[]) {
				size += writeArray(out,(boolean[])a);
			} else if (a instanceof Object[]) {
				size += writeList(out,(Object[])a);
			} else if (a instanceof List) {
				size += writeList(out,((List)a).toArray());
            } else if (a instanceof Map) {
                size += writeHash(out,(Map)a);
            } else if (a instanceof IStreamHolder) {
                size += writeStream(out,(IStreamHolder)a);
			} else {
				throw new BinStreamException("Can not write the object: "+obj.getClass().getName());
			}
		}
		return size;
	}
    
    public final static int writeHash(OutputStream out,Map map) throws IOException {
        out.write(T_HASH);
        writeInt4(out,map.size());
        int size = 5;
        for(Iterator it=map.keySet().iterator();it.hasNext();) {
            Object key = it.next();
            Object val = map.get(key);
            writeObject(out,key);
            writeObject(out,val);
        }
        return size;
    }

}
