require 'inourpc'
include INOURPC
cl = RPCClient.new("localhost",9999)
cl.start
cl.send_message("exit")
cl.shutdown
