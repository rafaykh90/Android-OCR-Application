package mcc_2016_g05_p2.niksula.hut.fi.rpc;

/* API for a minimal HTTP client */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

/* package */ abstract class IMinimalHTTPClient
{
	public String Host = "";
	public int Port = 0;
	public String Method = "";
	public String Route = "";
	public HashMap<String, String> ExtraHeaders = new HashMap<String, String>();
	public int ResponseContentLength = -1; // -1 if not available

	public boolean DoInput = false;
	public boolean DoOutput = false;

	public abstract void connect() throws IOException;
	public abstract void disconnect() throws IOException;
	public abstract InputStream getInputStream() throws IOException;
	public abstract OutputStream getOutputStream() throws IOException;
}
