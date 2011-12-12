require 'inourpc'

include INOURPC
count = 0

loop {
  cl = RPCClient.new("localhost",$*[0].to_i)
  cl.set_debug(true)
  cl.start

  cl.send_message("echo",10)
  #cl.send_message("echo","asasdafffffffffffffffffffffffffffakjsdlhfkajshfdlkasjhfdlkajsdhflkasjdhfkasjdhfffffffffffffffffffffffffffffffffffffffuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuaqswderthyjuikaqswdefrgthyjukiliopoiueproiqwjeoifdjslkdjf;laskjdf;laksj;dlfkj;alskdjf;laskjdf;lksajdf;lkasjdf;lkjas;lkdjf;laskjf;laksjdf;lksajdf;lksajdf;lkajds;lfkjdfasdfdsfasdf")

  cl.shutdown

  count += 1
  puts count if (count % 50) == 0
}
