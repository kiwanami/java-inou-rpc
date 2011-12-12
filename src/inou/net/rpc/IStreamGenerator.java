package inou.net.rpc;


import java.io.IOException;
import java.io.InputStream;

/**
   This interface decides how does this system receive 
   the byte array stream. If the size of a byte array is larger than
   maximum heap size, the INOU-RPC system fails to receive the data.
   Then, the RPC system should transform the large data into another
   form directly, such as file and network.
*/
public interface IStreamGenerator {

	/**
	 * transforming incoming date into the certain format.
	 *
	 * @param size length of byte array
	 * @param stream the InputStream object. this object is 
	 * @return the object that is sent by a rpc method argument.
	 * @exception IOException if an error occurs
	 */
	public Object transform(int size,InputStream stream) throws IOException;

}
