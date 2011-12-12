package inou.net.rpc;

import java.io.IOException;
import java.io.InputStream;


public class StreamHolderClass implements IStreamHolder {
	
	private final int size;
	private final InputStream stream;

	public StreamHolderClass(int size,InputStream is) {
		this.size = size;
		this.stream = is;
	}

	public final int getSize() {
		return size;
	}

	public final InputStream getStream() {
		return stream;
	}

	public void onFinished() throws IOException {
		stream.close();
	}

	public void onException(Exception e) {
		//do nothing
	}

}
