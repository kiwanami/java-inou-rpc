package inou.net.rpc;

import inou.net.RemoteRuntimeException;

import java.io.IOException;
import java.io.OutputStream;

public class ResultErrObject extends AbstractResultObject {

	private int code;
	private String err_klass, err_message, err_detail;

	public ResultErrObject(Object _sid,int c,String k,String m,String d) {
		super(_sid);
		code = c;
		err_klass = k;
		err_message = m;
		err_detail = d;
	}

	protected void exec_impl(OutputStream out) throws IOException {
		BinEncoder.writeObject(out,sid);
		BinEncoder.write(out,(byte)code);
		BinEncoder.write(out,err_klass);
		BinEncoder.write(out,err_message);
		BinEncoder.write(out,err_detail);
		out.flush();
	}

	Object getValue() throws BinStreamException,IOException {
		switch (code) {
		case R_APP_ERROR:
			throw new RemoteRuntimeException(err_klass,err_message,err_detail);
		case R_PROTOCOL_ERROR:
			throw new IOException("Communication error: class="+err_klass+"  message="+err_message+"\n"+err_detail);
		case R_FATAL_ERROR:
			throw new InternalError("Fatal error: class="+err_klass+"  message="+err_message+"\n"+err_detail);
		default:
			throw new RuntimeException("Unknown return code:"+code);
		}
	}

	public String toString() {
		String id = "UNKNOWN";
		switch(code) {
		case R_APP_ERROR:
			id = "APP_ERROR";
			break;
		case R_PROTOCOL_ERROR:
			id = "PROTOCOL_ERROR";
			break;
		case R_FATAL_ERROR:
			id = "FATAL_ERROR";
			break;
		}
		return "ResultErrObject: "+id+" : sid="+sid+"  class=: "+err_klass+" ["+err_message+"]";
	}
}