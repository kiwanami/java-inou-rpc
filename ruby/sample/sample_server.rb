require 'inourpc'
include INOURPC

STDOUT.sync = true

port = $*[0] || 0

logger = Logger.new(STDOUT)
logger.level = Logger::DEBUG

ms = RPCMultiServer.new(port)
ms.logger = logger
puts "port #{port}"

ms.add_handler("echo",lambda{|arg| arg})
ms.start

Thread.stop
