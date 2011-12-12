package inou.net.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BinDecoder implements BinConstants {

	public static int read(InputStream in,IDecodeHandler handler) throws IOException {
		return read(in,handler,null);
	}

	public static int read(InputStream in,IDecodeHandler handler,IStreamGenerator isg) throws IOException {
		return read(in,handler,0,isg);
	}

	private static int read(InputStream in,IDecodeHandler handler,int size,IStreamGenerator isg) throws IOException {
		int header = in.read();
		if (header == -1) return size;
		switch(header) {
		case T_NULL:
			return size+readNull(in,handler)+1;
		case T_BOOLEAN_TRUE:
			return size+readBool(in,handler,true)+1;
		case T_BOOLEAN_FALSE:
			return size+readBool(in,handler,false)+1;
		case T_INTEGER1:
			return size+readInt1(in,handler)+1;
		case T_INTEGER2:
			return size+readInt2(in,handler)+1;
		case T_INTEGER4:
			return size+readInt4(in,handler)+1;
		case T_INTEGER8:
			return size+readInt8(in,handler)+1;
		case T_FLOAT:
			return size+readFloat(in,handler)+1;
		case T_DOUBLE:
			return size+readDouble(in,handler)+1;
		case T_STRING:
			return size+readString(in,handler)+1;
		case T_DECIMAL:
			return size+readDecimal(in,handler)+1;
		case T_ARRAY:
			return size+readArray(in,handler,isg)+1;
		case T_LIST:
			return size+readList(in,handler,isg)+1;
        case T_HASH:
            return size+readHash(in,handler,isg)+1;
		}
		throw new BinStreamException("Decoder: wrong header:"+header);
	}

	private static long read_ulong(InputStream in) throws IOException {
		return (long)(in.read() & 0xff);
	}

	private static int read_uint(InputStream in) throws IOException {
		return (int)(in.read() & 0xff);
	}

	private static short read_ushort(InputStream in) throws IOException {
		return (short)(in.read() & 0xff);
	}

	private static byte read_ubyte(InputStream in) throws IOException {
		return (byte)in.read();
	}

	private static int readNull(InputStream in,IDecodeHandler handler) throws IOException {
		handler.readNull();
		return 0;
	}

	private static int readBool(InputStream in,IDecodeHandler handler,boolean a) throws IOException {
		handler.readBool(a);
		return 0;
	}

	private static int readInt1(InputStream in,IDecodeHandler handler) throws IOException {
		handler.readByte(read_ubyte(in));
		return 1;
	}

	private static int readInt2(InputStream in,IDecodeHandler handler) throws IOException {
		handler.readShort( (short)(read_ushort(in) | (read_ushort(in)<<8)) );
		return 2;
	}

	private static int readInt4Gen(InputStream in) throws IOException {
		return 
			read_uint(in) | (read_uint(in)<<8) | 
			(read_uint(in)<<16) | (read_uint(in)<<24);
	}

	private static int readInt4(InputStream in,IDecodeHandler handler) throws IOException {
		handler.readInt( readInt4Gen(in) );
		return 4;
	}

	private static long readInt8Gen(InputStream in) throws IOException {
		return
			 read_ulong(in) |
			(read_ulong(in) << 8) |
			(read_ulong(in) << 16) |
			(read_ulong(in) << 24) |
			(read_ulong(in) << 32) |
			(read_ulong(in) << 40) |
			(read_ulong(in) << 48) |
			(read_ulong(in) << 56);
	}

	private static int readInt8(InputStream in,IDecodeHandler handler) throws IOException {
		handler.readLong( readInt8Gen(in) );
		return 8;
	}

	private static int readFloat(InputStream in,IDecodeHandler handler) throws IOException {
		handler.readFloat( Float.intBitsToFloat( readInt4Gen(in) ) );
		return 4;
	}

	private static int readDouble(InputStream in,IDecodeHandler handler) throws IOException {
		handler.readDouble( Double.longBitsToDouble( readInt8Gen(in) ) );
		return 8;
	}

	private static byte[] readBytes(InputStream in) throws IOException {
		int length = readInt4Gen(in);
		if (length == -1) {
			return null;
		}
		byte[] ret = new byte[length];
		int count = 0;
		while(count < length) {
			count += in.read(ret,count,length-count);
		}
		return ret;
	}

	private static String readStringGen(byte[] a) throws IOException {
		try {
			return new String(a, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			//the exception will be never throwen.
			e.printStackTrace();
			return new String(a);
		}
	}

	private static int readString(InputStream in,IDecodeHandler handler) throws IOException {
		byte[] src = readBytes(in);
		if (src == null) {
			handler.readString(null);
		} else {
			handler.readString(readStringGen(src));
		}
		return src.length+4;
	}

	private static int readDecimal(InputStream in,IDecodeHandler handler) throws IOException {
		byte[] src = readBytes(in);
		handler.readDecimal(new BigDecimal(readStringGen(src)));
		return src.length+4;
	}

	//====(Array)====================================

	private static int readArray(InputStream in,IDecodeHandler handler,IStreamGenerator isg) throws IOException {
		int type = in.read();
		int num = readInt4Gen(in);
		switch(type) {
		case T_NULL:
			return readNullArray(in,handler,num)+5;
		case T_BOOLEAN_TRUE:
			return readBoolArray(in,handler,num)+5;
		case T_INTEGER1:
			return readInt1Array(in,handler,num,isg)+5;
		case T_INTEGER2:
			return readInt2Array(in,handler,num)+5;
		case T_INTEGER4:
			return readInt4Array(in,handler,num)+5;
		case T_INTEGER8:
			return readInt8Array(in,handler,num)+5;
		case T_FLOAT:
			return readFloatArray(in,handler,num)+5;
		case T_DOUBLE:
			return readDoubleArray(in,handler,num)+5;
		case T_DECIMAL:
			return readDecimalArray(in,handler,num)+5;
		case T_STRING:
			return readStringArray(in,handler,num)+5;
		}
		throw new BinStreamException("Decoder: wrong array type:"+type);
	}

	private static int readBoolArray(InputStream in,IDecodeHandler handler,int num) throws IOException {
		boolean[] ret = new boolean[num];
		int count = 0;
		for(int i=0;i<num;i++) {
			ret[i] = in.read() == T_BOOLEAN_TRUE;
		}
		handler.readArray(ret);
		return ret.length;
	}

	private static int readNullArray(InputStream in,IDecodeHandler handler,int num) throws IOException {
		Object[] ret = new Object[num];
		handler.readArray(ret);
		return ret.length;
	}

	private static int readInt1Array(InputStream in,IDecodeHandler handler,int num,IStreamGenerator isg) throws IOException {
		if (isg == null) {
			byte[] ret = new byte[num];
			int count = 0;
			while(count < num) {
				count += in.read(ret,count,num-count);
			}
			handler.readArray(ret);
		} else {
			handler.readObject(isg.transform(num,in));
		}
		return num;
	}

	private static int readInt2Array(InputStream in,IDecodeHandler handler,int num) throws IOException {
		short[] ret = new short[num];
		for(int i=0;i<num;i++) {
			short a0 = (short)in.read();
			short a1 = (short)in.read();
			ret[i] = (short)((a1<<8) + a0);
		}
		handler.readArray(ret);
		return ret.length*2;
	}

	private static int readInt4Array(InputStream in,IDecodeHandler handler,int num) throws IOException {
		int[] ret = new int[num];
		for(int i=0;i<num;i++) {
			ret[i] = readInt4Gen(in);
		}
		handler.readArray(ret);
		return ret.length*4;
	}

	private static int readInt8Array(InputStream in,IDecodeHandler handler,int num) throws IOException {
		long[] ret = new long[num];
		for(int i=0;i<num;i++) {
			ret[i] = readInt8Gen(in);
		}
		handler.readArray(ret);
		return ret.length*8;
	}

	private static int readFloatArray(InputStream in,IDecodeHandler handler,int num) throws IOException {
		float[] ret = new float[num];
		for(int i=0;i<num;i++) {
			ret[i] = Float.intBitsToFloat(readInt4Gen(in));
		}
		handler.readArray(ret);
		return ret.length*4;
	}

	private static int readDoubleArray(InputStream in,IDecodeHandler handler,int num) throws IOException {
		double[] ret = new double[num];
		for(int i=0;i<num;i++) {
			ret[i] = Double.longBitsToDouble(readInt8Gen(in));
		}
		handler.readArray(ret);
		return ret.length*8;
	}

	private static int readStringArray(InputStream in,IDecodeHandler handler,int num) throws IOException {
		String[] ret = new String[num];
		int size = 0;
		for(int i=0;i<num;i++) {
			byte[] src = readBytes(in);
			if (src == null) {
				ret[i] = null;
				size += 4;
			} else {
				ret[i] = readStringGen(src);
				size += src.length+4;
			}
		}
		handler.readArray(ret);
		return size;
	}

	private static int readDecimalArray(InputStream in,IDecodeHandler handler,int num) throws IOException {
		BigDecimal[] ret = new BigDecimal[num];
		int size = 0;
		for(int i=0;i<num;i++) {
			byte[] src = readBytes(in);
			ret[i] = new BigDecimal(readStringGen(src));
			size += src.length+4;
		}
		handler.readArray(ret);
		return size;
	}

	//====(List)====================================

	private static int readList(InputStream in,IDecodeHandler handler,IStreamGenerator isg) throws IOException {
		int num = readInt4Gen(in);
		final Object[] ret = new Object[num];
		int size = 4;
		IDecodeHandler lh = new IDecodeHandler() {
				int i=0;
				public void readNull() {
					ret[i++] = null;
				}
				public void readBool(boolean a) {
					ret[i++] = a ? Boolean.TRUE : Boolean.FALSE;
				}
				public void readByte(byte a) {
					ret[i++] = new Byte(a);
				}
				public void readShort(short a) {
					ret[i++] = new Short(a);
				}
				public void readInt(int a) {
					ret[i++] = new Integer(a);
				}
				public void readLong(long a) {
					ret[i++] = new Long(a);
				}
				public void readFloat(float a) {
					ret[i++] = new Float(a);
				}
				public void readDouble(double a) {
					ret[i++] = new Double(a);
				}
				public void readString(String a) {
					ret[i++] = a;
				}
				public void readDecimal(BigDecimal a) {
					ret[i++] = a;
				}

				public void readArray(boolean[] a) {
					ret[i++] = a;
				}
				public void readArray(byte[] a) {
					ret[i++] = a;
				}
				public void readArray(short[] a) {
					ret[i++] = a;
				}
				public void readArray(int[] a) {
					ret[i++] = a;
				}
				public void readArray(long[] a) {
					ret[i++] = a;
				}
				public void readArray(float[] a) {
					ret[i++] = a;
				}
				public void readArray(double[] a) {
					ret[i++] = a;
				}
				public void readArray(String[] a) {
					ret[i++] = a;
				}
				public void readArray(BigDecimal[] a) {
					ret[i++] = a;
				}
				public void readArray(Object[] a) {
					ret[i++] = a;
				}

				public void readList(Object[] a) {
					ret[i++] = a;
				}
                public void readHash(Map map) {
                    ret[i++] = map;
                }
				public void readObject(Object obj) {
					ret[i++] = obj;
				}
			};
		for(int j=0;j<num;j++) {
			size += read(in,lh,isg);
		}
		handler.readList(ret);
		return size;
	}
    
    public final static int readHash(InputStream in,IDecodeHandler handler,IStreamGenerator isg) throws IOException {
        HashMap ret = new HashMap();
        int num = readInt4Gen(in);
        int size = 4;
        for(int i=0;i<num;i++) {
            DecodeHandlerClass2 key = new DecodeHandlerClass2();
            DecodeHandlerClass2 value = new DecodeHandlerClass2();
            size += read(in,key,isg);
            size += read(in,value,isg);
            ret.put(key.getObject(),value.getObject());
        }
        handler.readHash(ret);
        return size;
    }

}
