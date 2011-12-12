require 'inourpc'
include INOURPC

logger = Logger.new(STDOUT)
logger.level = Logger::DEBUG
logger.datetime_format = get_logger_format("LOG")

ms = RPCMultiServer.new(9999)
ms.logger = logger

simple = lambda{|b| puts b; b}

[
  "echo_boolean",
  "echo_int1",
  "echo_int2",
  "echo_int4",
  "echo_int8",
  "echo_string",
  "echo_null",
  "echo_decimal",
  "echo_float",
  "echo_double",

  "echo_boolean_array",
  "echo_int1_array",
  "echo_int2_array",
  "echo_int4_array",
  "echo_int8_array",
  "echo_string_array",
  "echo_null_array",
  "echo_decimal_array",
  "echo_float_array",
  "echo_double_array",

  "echo_list",
  "echo_hash",
].each {|i| ms.add_handler(i,simple)}

ms.add_handler("exit",lambda { ms.shutdown })
ms.start
