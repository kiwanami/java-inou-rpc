package inou.net.rpc;

import java.io.IOException;
import java.io.OutputStream;


public abstract class AbstractTransferObject {

	//Calling session ID.
	public final Object sid;

	/**
	 * Creates a new <code>AbstractTransferObject</code> instance.
	 *
	 * @param sessionId session ID between calling methong and returning result.
	 */
	protected AbstractTransferObject(Object sessionId) {
		sid = sessionId;
	}

	protected abstract void exec_impl(OutputStream out) throws IOException;

	protected abstract byte getMessageType();

	void exec(OutputStream out) throws IOException {
		BinEncoder.write(out,getMessageType());
		exec_impl(out);
		out.flush();
	}

}
