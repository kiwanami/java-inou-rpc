package inou.net.rpc;

import java.net.InetAddress;

/**
   BinServer decides whether incomming connections are valid or not,
   asking to this class.
*/
public interface IConnectionAccepter {

	/**
	   if true, the server grants the host to access.
	*/
    public boolean accepts(InetAddress address);

}
