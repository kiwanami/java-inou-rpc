package inou.net.rpc;

import java.util.HashMap;


public class ServiceManager {

	private HashMap handlerTable = new HashMap();
    private ServiceManager parentManager;
	private IStreamGenerator streamGenerator;

	public ServiceManager() {
	}

    public ServiceManager(ServiceManager p) {
        parentManager = p;
    }

	public void setStreamGenerator(IStreamGenerator isg) {
		streamGenerator = isg;
	}

	public IStreamGenerator getStreamGenerator() {
		if (streamGenerator != null) {
			return streamGenerator;
		} else if (parentManager != null) {
			return parentManager.getStreamGenerator();
		} else {
			return null;
		}
	}

	void setParentManager(ServiceManager p) {
		parentManager = p;
	}

	public synchronized void addHandler(String name,IMessageHandler h) {
		handlerTable.put(name,h);
	}

	public synchronized void removeHandler(String name) {
		handlerTable.remove(name);
	}

	public synchronized IMessageHandler getHandler(String name) {
		IMessageHandler h = (IMessageHandler)handlerTable.get(name);
        if (h == null && parentManager != null) {
            return parentManager.getHandler(name);
        }
        return h;
	}

}
