import java.io.IOException;
import java.io.FileInputStream;
import java.net.ServerSocket;

import inou.net.rpc.ICommunicator;
import inou.net.rpc.IMessageHandler;
import inou.net.rpc.MultiBinServer;

import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ServerSocketFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;
import java.security.UnrecoverableKeyException;


public class SSLSampleServer {

    public static void main(String[] args) throws IOException,KeyStoreException,NoSuchAlgorithmException,KeyManagementException,CertificateException,UnrecoverableKeyException {
        KeyStore ks = KeyStore.getInstance( "JKS" );
        char[] keystorePass = "storepass".toCharArray();
        ks.load( new FileInputStream( "./ssl_test/server/server_keystore" ) , keystorePass );
        
        char[] keyPass = "password".toCharArray();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
        kmf.init( ks, keyPass );

        SSLContext sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( kmf.getKeyManagers() , null , null );
        ServerSocketFactory ssf = sslContext.getServerSocketFactory();
        ServerSocket serverSocket  = ssf.createServerSocket(10024);
        
        //==================================================

        MultiBinServer serverGenerator = new MultiBinServer(serverSocket);
        
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
