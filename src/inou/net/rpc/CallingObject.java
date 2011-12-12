package inou.net.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStream;
import java.io.OutputStream;


public class CallingObject extends AbstractTransferObject implements BinConstants {

	public final String name;
	public final Object[] args;

	public String toString() {
		return "CallingObject: sid="+sid+" name="+name+" args:"+args.length;
	}

	public CallingObject(Object s,String n,Object[] a) {
		super(s);
		name = n;
		if (a == null) {
			a = new Object[0];
		}
		args = a;
	}

	static CallingObject getCallingObject(InputStream in,IStreamGenerator isg) throws IOException {
		DecodeHandlerClass2 ret = new DecodeHandlerClass2();
		BinDecoder.read(in,ret);
		Object sid = ret.getValue();

		BinDecoder.read(in,ret);
		String name = (String)ret.getValue();
		
		BinDecoder.read(in,ret,isg);
		Object[] args = ret.getList();
		return new CallingObject(sid,name,args);
	}

	protected byte getMessageType() {
		return (byte)M_CALL;
	}

	protected void exec_impl(OutputStream out) throws IOException {
		BinEncoder.writeObject(out,sid);
		BinEncoder.write(out,name);
		BinEncoder.writeList(out,args);
		out.flush();
	}
}
