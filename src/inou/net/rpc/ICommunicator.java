package inou.net.rpc;

import java.io.IOException;
import java.net.InetAddress;

public interface ICommunicator extends IServiceManager {

	public void setStreamGenerator(IStreamGenerator isg);

	public void start();
	public void shutdown();

    public InetAddress getRemoteAddress();

    public boolean isConnected();

	public Object send(String name,Object[] args) throws IOException;

}
