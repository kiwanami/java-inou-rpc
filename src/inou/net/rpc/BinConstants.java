
package inou.net.rpc;

public interface BinConstants {

	public static final int M_NONE     =   -1;
	public static final int M_CALL     = 0x00;
	public static final int M_RETURN   = 0x01;

	public static final int T_NULL     = 0x10;
	public static final int T_INTEGER1 = 0x11;
	public static final int T_INTEGER2 = 0x12;
	public static final int T_INTEGER4 = 0x13;
	public static final int T_INTEGER8 = 0x14;
	public static final int T_FLOAT    = 0x15;
	public static final int T_DOUBLE   = 0x16;
	public static final int T_DECIMAL  = 0x17;
	public static final int T_STRING   = 0x18;
	public static final int T_ARRAY    = 0x19;
	public static final int T_LIST     = 0x1a;
    public static final int T_HASH     = 0x1d;

	public static final int T_BOOLEAN_TRUE  = 0x1b;
	public static final int T_BOOLEAN_FALSE = 0x1c;

	public static final int R_OK              = 0x20;
	public static final int R_PROTOCOL_ERROR  = 0x21;
	public static final int R_APP_ERROR       = 0x22;
	public static final int R_FATAL_ERROR     = 0x23;

}
