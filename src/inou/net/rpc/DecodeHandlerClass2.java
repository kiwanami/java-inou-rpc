package inou.net.rpc;

import java.math.BigDecimal;
import java.util.Map;


public class DecodeHandlerClass2 implements IDecodeHandler {

	private Object value;
	private Object[] list;
    private Map map;

	public Object getValue() {
		return value;
	}

	public Object[] getList() {
		return list;
	}

    public Object getObject() {
        if (map != null) {
			return map;
		} else if (list != null) {
			return list;
		} else {
			return value;
		}
    }

	public void readNull() {
		value = null;
	}
	public void readBool(boolean a) {
		value = a ? Boolean.TRUE : Boolean.FALSE;
	}
	public void readByte(byte a) {
		value = new Byte(a);
	}
	public void readShort(short a) {
		value = new Short(a);
	}
	public void readInt(int a) {
		value = new Integer(a);
	}
	public void readLong(long a) {
		value = new Long(a);
	}
	public void readFloat(float a) {
		value = new Float(a);
	}
	public void readDouble(double a) {
		value = new Double(a);
	}
	public void readString(String a) {
		value = a;
	}
	public void readDecimal(BigDecimal a) {
		value = a;
	}

	public void readArray(boolean[] a) {
		value = a;
	}
	public void readArray(byte[] a) {
		value = a;
	}
	public void readArray(short[] a) {
		value = a;
	}
	public void readArray(int[] a) {
		value = a;
	}
	public void readArray(long[] a) {
		value = a;
	}
	public void readArray(float[] a) {
		value = a;
	}
	public void readArray(double[] a) {
		value = a;
	}
	public void readArray(String[] a) {
		value = a;
	}
	public void readArray(BigDecimal[] a) {
		value = a;
	}
	public void readArray(Object[] a) {
		value = a;
	}

	public void readList(Object[] a) {
		list = a;
	}
    public void readHash(Map map) {
        this.map = map;
    }

	public void readObject(Object obj) {
		value = obj;
	}
}
