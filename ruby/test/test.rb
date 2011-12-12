# 
# INOU RPC Test
# 

require 'test/unit'
require 'stringio'
require 'inourpc'
require 'pp'

include INOURPC

class EncoderTest < Test::Unit::TestCase
  include Encoder,Decoder

  def setup
  end

  def writes(a)
	@out << a
	return a.size
  end

  def test_list
	@out = StringIO.new
	write_list([])
	@out.rewind
	read_block(@out) {|type,val|
	  assert_equal(T_LIST,type)
	  assert_equal([],val)
	}
  end

  def test_array
	@out = StringIO.new
	check_array_gen( [true,false,false,true] )
	@out = StringIO.new
	check_array_gen( [1,-1,4,-4] )
	@out = StringIO.new
	check_array_gen( [551,-551,774,-774] )
	@out = StringIO.new
	check_array_gen( [65539,-65539] )
	@out = StringIO.new
	check_array_gen( [(1<<33)+10 , -(1<<33)-10 ] )
	@out = StringIO.new
	check_array_gen( [(1<<65)+10 , -(1<<65)-10] )
	@out = StringIO.new
	check_array_gen( [1.12 , -0.000112] )
	@out = StringIO.new
	check_array_gen( ["Hello","World","OK?","<>"] )
	@out = StringIO.new
	check_array_gen( [-12323452355,1,"OK?",1.2] )
	@out = StringIO.new
	check_array_gen( [] )
	@out = StringIO.new
	check_array_gen( [nil,nil,nil] )
  end

  def check_array_gen(sample)
	write(sample)
	@out.rewind
	read_block(@out) {|type,val|
	  assert_equal(sample,val)
	}
  end

  def test_typed_array
	@out = StringIO.new
	check_typed_array_gen( [:t_int4,0,1,2] )
	@out = StringIO.new
	check_typed_array_gen( [:t_int4] )
	@out = StringIO.new
	check_typed_array_gen( [:t_int1,1,2,3] )
	@out = StringIO.new
	check_typed_array_gen( [:t_int2,1,2,3] )
	@out = StringIO.new
	check_typed_array_gen( [:t_int8,1,2,3] )
	@out = StringIO.new
	check_typed_array_gen( [:t_float,1,2,3] )
	@out = StringIO.new
	check_typed_array_gen( [:t_double,1,2,3] )
	@out = StringIO.new
	check_typed_array_gen( [:t_decimal,1,2,3] )
	@out = StringIO.new
	check_typed_array_gen( [:t_string,"1","2","3"] )
	@out = StringIO.new
	check_typed_array_gen( [:t_boolean,true,false,true] )
  end

  def check_typed_array_gen(sample)
	write(sample)
	sample.shift
	@out.rewind
	read_block(@out) {|type,val|
	  assert_equal(sample,val)
	}
  end

  def test_primitive
	@out = StringIO.new
	sample = [
	  false, true, nil,
	  1,-1,513,-515,65539,-65539, 
	  (1<<33)+10 , -(1<<33)-10 , 
	  (1<<65)+10 , -(1<<65)-10 , 1.12 , -0.000112,
	  "Hello world!"]
	sample.each {|i|
	  write(i)
	}
	@out.rewind
	sample.each {|i|
	  read_block(@out) {|type,val|
		assert_equal(i,val)
	  }
	}
  end

  def test_hash
	@out = StringIO.new
	hash = Hash.new
	hash["key1"]=1
	hash["key2"]=2
	hash["key3"]="String"
	hash[4]=[1,2,3,"a"]
	write(hash)
	@out.rewind
	type,val = read(@out)
	assert_equal(T_HASH,type)
	assert_equal(4,hash.size)
	assert_equal(hash,val)
  end

  def test_nest_list
	@out = StringIO.new
	sample = [
	  1,[2,3],[3,[4,5]],[6,[7,[8,9]],10]
	]
	write(sample)
	@out.rewind
	type,ret = read(@out)
	sample.zip(ret).each {|s,t|
	  assert_equal(s,t)
	}
  end

  def test_packed_string
	@out = StringIO.new
	sample = "abcdef\0\1\2"
	write([:t_int1,sample])
	@out.rewind
	type,ret = read(@out)
	sample.unpack("c*").zip(ret).each {|s,t|
	  assert_equal(s,t)
	}
  end

end
