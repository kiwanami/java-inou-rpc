package inou.net.rpc;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileStreamGenerator implements IStreamGenerator {

	private File directory = null;

	public FileStreamGenerator() {
	}

	public FileStreamGenerator(File dir) {
		directory = dir;
	}

	public Object transform(int size,InputStream in) throws IOException {
		File f = File.createTempFile("inourpc",".tmp",directory);

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
		int count = 0;
		byte[] buf = new byte[Math.min(size,1024*8)];
		while(true) {
			int len = in.read(buf,0,Math.min(size-count,buf.length));
			if (len == -1) {
				throw new EOFException("Unexcepted EOF during reading stream data (size="+size+" : count="+count+")");
			}
			out.write(buf,0,len);
			count += len;
			if (count == size) {
				break;
			}
		}
		out.flush();
		out.close();

		return f;
	}

}
