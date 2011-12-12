package inou.net.rpc;

import java.io.IOException;

import java.io.OutputStream;


public class ResultOkObject extends AbstractResultObject {

	public final Object value;

	public ResultOkObject(Object _sid,Object _value) {
		super(_sid);
		value = _value;
	}

	protected void exec_impl(OutputStream out) throws IOException {
		BinEncoder.writeObject(out,sid);
		BinEncoder.write(out,(byte)R_OK);
		BinEncoder.writeObject(out,value);
		out.flush();
	}

	Object getValue() throws BinStreamException {
		return value;
	}

	public String toString() {
		return "ResultObject: OK : sid="+sid+"  value=: "+value;
	}

}
