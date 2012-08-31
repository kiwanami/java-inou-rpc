INOU-RPC

  Integrated Numerical Operation Utilities,
  RPC Extension.

  Version 0.4.3
  Time-stamp: "2012-08-31 14:15:33 sakurai"

■何か？

Javaの基本型とその配列、ListとHashをサポートした簡易RPC。
バイナリーエンコーディングで、RPCごとに接続し直さないためXMLRPCよりも
高速で、双方向からの呼び出しが可能。

■使用例

参照： sample/SampleServer.java, SampleClient.java

○サーバー

  MultiBinServer serverGenerator = new MultiBinServer(10024);//port=10024
          
  //やってきた文字列を加工して返すメソッド　echo を追加
  serverGenerator.addHandler("echo",new IMessageHandler() {
     public Object send(Object[] args) throws Exception {
         return "ECHO: "+args[0];
     }
  });
          
  //クライアントが接続してくるのを待つ
  ICommunicator client = serverGenerator.getClientConnection();
  
○クライアント

  BinClient client = new BinClient("localhost",10024);
  
  try {
      client.start();//接続開始
  } catch (RuntimeException e) {
      //相手がいなくて timeout したときなど。
      e.printStackTrace();
      return;
  }
  
  Object obj = null;
  try {
      //server側の echo メソッドを引数 hello で実行
      obj = client.send("echo",new Object[]{"hello"});
  } catch (RemoteRuntimeException e) {
      //サーバー側のプログラムでエラーが起きた場合
      e.printStackTrace();
  } catch (IOException e) {
      //通信自体に何か問題があった場合
      e.printStackTrace();
  }
  System.out.println(obj);
  
  client.shutdown();

■インストールと設定

inourpc.jar をクラスパスに加える。
また、動作にはlog4jも必要。

log4jの設定は conf/log4j.properties を参照。
通常はlog4j.logger.inou.netについてWARN以上を設定する。

■使用方法詳細

○サーバー側：MultiBinServer

・起動

起動にはポート番号を指定する方法と、指定しない方法がある。
指定しなければ空いているポートを探してきて使う。
何番を使用しているかは getPortNumber() で得られる。

・アクセス制限

IConnectionAccepter を実装したオブジェクトを setConnectionAccepter で
設定する。実装例としてローカルホストのみからの接続を許可する
 LocalhostAccepter がある。

・RPCハンドラの設定

IMessageHandler を実装したオブジェクトを addHandler で設定する。
ハンドラオブジェクトは複数の接続で共有されるので、ステートレスか
スレッドセーフに作っておく必要がある。

もし、各接続ごとにハンドラオブジェクトを分けたい場合は、 
getClientConnection の引数に入れる ServiceManager を毎回newして 
addHandler する。ServiceManager には親子関係があって、 MultiBinServer 
でaddHandler したものは各接続で共有されて、各接続でハンドラを定義した
場合は、その接続のみ有効になる。同じ名前のハンドラは、各接続で定義した
ものが優先される。

 getClientConnection した後で addHandler してもよいが、その場合はクラ
イアントが接続してくる前に addHandler が終了している必要がある。

○クライアント側：BinClient

・サーバーへ接続

コンストラクターでホスト名とポート番号を指定して、 start すると接続開
始。 setReconnect(true) で再接続をするようになる（初期設定はfalse）が、
ステートフルな接続である場合、接続部分の抽象化をしておかなければ再接続しても
あまり意味が無いかも。

setConnectionTimeoutMiliSec で接続までのタイムアウト時間を設定できる。
デフォルトは 2000msec になっているので、サーバーの応答が遅い場合は
30秒とかに設定しておく必要がある。

○サポートされるデータ型

null
Boolean, String, boolean[], String[]
Byte, Short, Integer, Long, BigDecimal, Float, Double
byte[], short[], int[], long[], BigDecimal[], float[], double[]
List, HashMap
上記のどれかが入った Object[]

○大容量データの転送

巨大なファイルを送りたい場合、普通に byte[] するとヒープメモリーがいっ
ぱいになるので OutOfMemoryError になる。その場合はストリーム形式を使っ
て、メモリーに全部を乗せないように送受信することが出来る。注意として、
あらかじめサイズのわからないものには使えない。あくまでRPCなので byte[] 
の別形式として使う。

別解としてプロトコル的に巨大データを切り出して受信側で結合するという設
計にするという方法もある。

・送信側

IStreamHolder を適当に実装して引数か帰り値に入れる。
onFinished は送信終了時に close したいときとかに使う。
onException は送信中にIOエラーが発生したときに、ファイルなどを一次退避
させたいときとかに使う。
適当な実装としてStreamHolderClassがある。onFinished は close して、
onException は何もしない実装になっている。必要に応じてオーバーライドし
て使うと便利。

・受信側

IStreamGenerator を実装して、BinClient か 
MultiBinServer.getClientConnection で得たオブジェクトに
setStreamGenerator で設定する。 IStreamGenerator は、単に
InputStreamで得た byte[] を別のオブジェクトに変換するもの。

適当な実装として、 FileStreamGenerator がある。これは、単に 
InputStream をローカルの一時ファイルに書き出す。出来たファイルを消すの
は各ファイルを受け取ったハンドラの責任なので、使い終わったら消すなり移
動する必要がある。

○Ruby実装

MultiBinServer, BinClient に対応するRuby実装がある。
Java版とほぼ同等なデータ型をサポートしており、ほぼ透過的に
JavaのサーバーとRPC通信することが出来る。
