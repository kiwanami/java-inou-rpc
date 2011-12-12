package inou.net.rpc;

public interface IServiceManager {

    public void addHandler(String name,IMessageHandler h);
    public void removeHandler(String name);

}
