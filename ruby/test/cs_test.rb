require 'test/unit'
require 'stringio'
require 'inourpc'
require 'pp'

include INOURPC

class CSTest < Test::Unit::TestCase

  def setup
	@server = RPCMultiServer.new(0)
	Thread.start {
	  @server.start
	}
	sleep(1)
  end
  
  def teardown
	@server.shutdown
  end	
  
  def test_simple
	@server.add_handler("echo",lambda{|arg| arg})
	client = RPCClient.new("localhost",@server.get_port_number)
	client.start
	
	res = client.send_message("echo","hello")
	assert_equal("hello",res)
	
	client.shutdown
  end

  def test_reconnect
	@server.add_handler("echo",lambda{|arg| arg})
	client = RPCClient.new("localhost",@server.get_port_number)
	client.reconnect = true
	client.start
	
	res = client.send_message("echo","hello")
	assert_equal("hello",res)
	
	@server.shutdown
	sleep(1)
	Thread.start {
	  @server.start
	}
	sleep(1)
	
	res = client.send_message("echo","hello")
	assert_equal("hello",res)

	client.shutdown
  end
  
end
