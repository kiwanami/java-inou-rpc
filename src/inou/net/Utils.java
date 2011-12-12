package inou.net;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This class has some utility methods for string manipilation and debug output. 
 */
public class Utils {

	public static Class[] object2class(Object[] args) {
		if (args == null) return null;
		Class[] ret = new Class[args.length];
		for(int i=0;i<ret.length;i++) {
			if (args[i] == null) {
				ret[i] = null;
			} else {
				ret[i] = args[i].getClass();
			}
		}
		return ret;
	}

	public static String trace2str(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		sw.flush();
		return sw.toString();
	}


	//====(debug)=====================================================

    public static String dumpBinData(byte[] array) {
        StringBuffer sb = new StringBuffer("====(binary dump)======\n");
        for (int i=0;i<(array.length/8+1);i++) {
            int max = Math.min(8,array.length-i*8);
            for (int j=0;j<max;j++) {
                int a = array[j+i*8];
                if (a < 0) a = 256+a;
                String hex = Integer.toHexString(a);
                if (hex.length() < 2) {
                    hex = "0"+hex;
                }
                sb.append(hex+" ");
            }
            sb.append(" |");
            try {
                sb.append(new String(array,i*8,max,"SJIS"));
            } catch (UnsupportedEncodingException e){
                sb.append(new String(array,i*8,max));
            }
            sb.append("|\n");
        }
        sb.append("====================");
        return sb.toString();
    }
    
    public static String conbine(Object [] ss) {
        String [] args = new String[ss.length];
        for (int i=0;i<args.length;i++) {
            if (ss[i] == null) {
                args[i] = "null";
            } else {
                args[i] = ss[i].toString();
            }
        }
        return conbine(args,"");
    }

    public static String conbine(String [] ss) {
        return conbine(ss,"");
    }

    public static String conbine(String [] ss,String spliter) {
        StringBuffer ret = new StringBuffer();
        for (int i=0;i<ss.length;i++) {
            ret.append(ss[i]).append(spliter);
        }
        if (ss.length == 0) {
            return ret.toString();
        }
        return ret.toString().substring(0,ret.length()-spliter.length());
    }

	public static void writeArray(Logger mon,Level writeLevel,Object[] args) {
        if (mon.getEffectiveLevel().isGreaterOrEqual(writeLevel)) {
			mon.log(writeLevel,conbine(args));
		}
	}

	public static void writeArguments(Logger mon,Level writeLevel,Object[] messages,Object[] args) {
		if (mon.getEffectiveLevel().isGreaterOrEqual(writeLevel)) {
			mon.log(writeLevel,makeArgumentExp(args,conbine(messages)+"( "," )"));
		}
	}
	
	public static void writeArguments(Logger mon,Level writeLevel,String message,List args) {
        if (mon.getEffectiveLevel().isGreaterOrEqual(writeLevel)) {
			mon.log(writeLevel,makeArgumentExp(args.toArray(),message+"( "," )"));
		}
	}

	public static void writeArguments(Logger mon,Level writeLevel,String message,Object[] args) {
        if (mon.getEffectiveLevel().isGreaterOrEqual(writeLevel)) {
			mon.log(writeLevel,makeArgumentExp(args,message+"( "," )"));
		}
	}

	public static String makeArgumentExp(Object[] args) {
		return makeArgumentExp(args,"(",")");
	}

	public static String makeArgumentExp(Object[] args,String begin,String end) {
		StringBuffer sb = new StringBuffer(begin);
		if (args == null || args.length == 0) {
			sb.append("");
		} else {
			for(int i=0;i<args.length;i++) {
				if (args[i] != null) {
					sb.append(args[i].toString());
				} else {
					sb.append("null");
				}
				if (i != (args.length-1)) {
					sb.append(", ");
				}
			}
		}
		sb.append(end);
		return sb.toString();
	}

}
