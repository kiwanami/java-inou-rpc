package inou.net.rpc;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class LocalhostAccepter implements IConnectionAccepter {

    public boolean accepts(InetAddress address) {
        try {
            return address.isLoopbackAddress() || InetAddress.getLocalHost().equals(address);
        } catch (UnknownHostException e) {
        }
        return false;
    }

}
