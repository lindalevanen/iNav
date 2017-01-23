package fi.aalto.tshalaa1.inav.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

public abstract class Request<V> implements Callable<V> {

	private InputStream in;
	private OutputStream out;
	
	void prepare(InputStream is, OutputStream os) {
		in = is;
		out = os;
	}
	
	@Override
	public V call() throws Exception {
		writeRequest(out);
		return readResponse(in);
	}

	protected abstract V readResponse(InputStream in);

	protected void writeRequest(OutputStream out) {
		
	}

}
