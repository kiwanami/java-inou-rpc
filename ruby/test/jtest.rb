# 
# INOU RPC Test (between ruby and java)
# 

require 'test/unit'
require 'inourpc'

include INOURPC

class EchoTest < Test::Unit::TestCase

  def setup
	@client = RPCClient.new("localhost",9999)
	#@client.set_debug(true)
	@client.start
  end

  def teardown
	@client.shutdown
  end

  def test_primitive
	sample = [
	  [:t_null, nil],
	  [:t_boolean, false],
	  [:t_boolean, true],
	  [:t_int2, 1,-1,513],
	  [:t_int2, -515],
	  [:t_int4, 65539],
	  [:t_int4, -65539],
	  [:t_int8, (1<<33)+10] ,
	  [:t_int8, -(1<<33)-10] ,
	  [:t_decimal, (1<<65)+10] ,
	  [:t_decimal, -(1<<65)-10] ,
	  [:t_float,1.1201],
	  [:t_float, -0.000112],
	  [:t_double,1.1201],
	  [:t_double, -0.000112],
	  [:t_string,"Hello world!"],
	  [:t_string,nil,nil,"A",nil],
	]
	sample.each {|t,v|
	  name = "echo_"+t.to_s[2..-1]
	  ret = @client.send_message(name,v)
	  if t == :t_float
		assert( (v-ret).abs < v.abs*0.001, "#{name} : #{v} -> #{ret}")
	  else
		assert_equal(v,ret,name)
	  end
	}
  end

  def check_array_gen( sample )
	type = sample[0]
	name = "echo_"+type.to_s[2..-1]+"_array"
	begin
	  ret = @client.send_message(name,sample)
	  if type == :t_float
		sample[1..-1].zip(ret).each {|i,j|
		  assert( (i-j).abs < j.abs*0.001, "#{name} : #{i} -> #{j}")
		}
	  else
		assert_equal(sample[1..-1],ret,"#{name} : #{sample[1..-1].join(', ')} -> #{ret.join(', ')}")
	  end
	rescue RPCException => e
	  puts e.klass
	  puts e.message
	  puts e.detail
	  raise e
	end
  end

  def test_array
	begin
	  check_array_gen( [:t_boolean,  true,false,false,true] )
	  check_array_gen( [:t_int1, 1,-1,4,-4] )
	  check_array_gen( [:t_int2, 551,-551,774,-774] )
	  check_array_gen( [:t_int4, 65539,-65539] )
	  check_array_gen( [:t_int8, (1<<33)+10 , -(1<<33)-10 ] )
	  check_array_gen( [:t_decimal, (1<<65)+10 , -(1<<65)-10] )
	  check_array_gen( [:t_float, 1.12 , -0.000112] )
	  check_array_gen( [:t_double, 1.12 , -0.000112] )
	  check_array_gen( [:t_string, "Hello","World","OK?","<>"] )
	  check_array_gen( [:t_int1] )
	  check_array_gen( [:t_string, nil,nil,"print"] )
	rescue => e
	  #puts e.klass
	  #puts e.message
	  #puts e.detail
	  raise e
	end
  end

  def test_hash
	hash = Hash.new
	hash["key1"]=1
	hash["key2"]=2
	hash["key3"]="String"
	hash[4]=[1,2,3,"a"]

	ret = @client.send_message("echo_hash",hash)

	assert_equal(hash,ret)
  end

  def test_nest_list
	sample = [
	  1,[2,3],[3,[4,5]],[6,[7,[8,9]],10]
	]

	ret = @client.send_message("echo_list",sample)

	sample.zip(ret).each {|s,t|
	  assert_equal(s,t)
	}
  end
  
end
