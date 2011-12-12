package inou.net.rpc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.io.InputStream;

public class DecodeHandlerClass implements IDecodeHandler {

	public void readNull() {};

	public void readBool(boolean a) {};
	public void readByte(byte a) {};
	public void readShort(short a) {};
	public void readInt(int a) {};
	public void readLong(long a) {};
	public void readFloat(float a) {};
	public void readDouble(double a) {};
	public void readString(String a) {};
	public void readDecimal(BigDecimal a) {};

	public void readArray(boolean[] a) {};
	public void readArray(byte[] a) {};
	public void readArray(short[] a) {};
	public void readArray(int[] a) {};
	public void readArray(long[] a) {};
	public void readArray(float[] a) {};
	public void readArray(double[] a) {};
	public void readArray(String[] a) {};
	public void readArray(BigDecimal[] a) {};
	public void readArray(Object[] a) {};

	public void readList(Object[] list) {}
    public void readHash(Map map) {}

	public void readObject(Object obj) {}
}
