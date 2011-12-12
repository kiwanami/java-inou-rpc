require 'inourpc'

include INOURPC

cl = RPCClient.new("localhost",9999)
cl.start

puts cl.send_message("add",10,20)
puts cl.send_message("sub",10,20)

cl.shutdown
