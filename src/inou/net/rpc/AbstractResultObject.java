package inou.net.rpc;

import java.io.IOException;
import java.io.InputStream;


abstract public class AbstractResultObject extends AbstractTransferObject implements BinConstants  {

	protected AbstractResultObject(Object _sid) {
		super(_sid);
	}

	public static AbstractResultObject get(InputStream in,IStreamGenerator isg) throws IOException {
		DecodeHandlerClass2 ret = new DecodeHandlerClass2();
		BinDecoder.read(in,ret);
		Object sid = ret.getValue();
		final int[] mcode = {0};
		BinDecoder.read(in,new DecodeHandlerClass() {
				public void readByte(byte c) {
					mcode[0] = c;
				}});
		switch (mcode[0]) {
		case R_OK:
            ret = new DecodeHandlerClass2();
			BinDecoder.read(in,ret,isg);
			Object value = ret.getObject();
			return new ResultOkObject(sid,value);
		case R_APP_ERROR:
		case R_PROTOCOL_ERROR:
		case R_FATAL_ERROR:
            ret = new DecodeHandlerClass2();
			BinDecoder.read(in,ret);
			String klass = (String)ret.getObject();
            ret = new DecodeHandlerClass2();
			BinDecoder.read(in,ret);
			String message = (String)ret.getObject();
            ret = new DecodeHandlerClass2();
			BinDecoder.read(in,ret);
			String detail = (String)ret.getObject();
			return new ResultErrObject(sid,mcode[0],klass,message,detail);
		default:
			throw new RuntimeException("Unknown return code:"+mcode[0]);
		} 
	}

	protected byte getMessageType() {
		return (byte)M_RETURN;
	}

	//void exec_impl(OutputStream out) throws IOException;

	abstract Object getValue() throws BinStreamException,IOException;
	
}
