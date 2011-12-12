import java.io.IOException;

import inou.net.rpc.ICommunicator;
import inou.net.rpc.IMessageHandler;
import inou.net.rpc.MultiBinServer;

public class SampleServer {

    public static void main(String[] args) throws IOException {
        MultiBinServer serverGenerator = new MultiBinServer(10024);//port=10024
        
        //やってきた文字列を加工して返すメソッド　echo を追加
        serverGenerator.addHandler("echo",new IMessageHandler() {
            public Object send(Object[] args) throws Exception {
                return "ECHO: "+args[0];
            }
        });
        
        //クライアントが接続してくるのを待つ
        ICommunicator client = serverGenerator.getClientConnection();
        
        //クライアント側のメソッド add を引数 1,2 で実行
        Object obj = client.send("add",new Object[]{new Integer(1),new Integer(2)});
        
        System.out.println(obj);
        
        //この後クライアント側から切断される。
        //サーバーのポートはまだ占有されているので、Ctrl-Cで止める。
    }

}
