package inou.net.rpc;

import java.io.IOException;
import java.io.InputStream;


public interface IStreamHolder {

	public int getSize();
	public InputStream getStream();

	public void onFinished() throws IOException;
	public void onException(Exception e);

}
