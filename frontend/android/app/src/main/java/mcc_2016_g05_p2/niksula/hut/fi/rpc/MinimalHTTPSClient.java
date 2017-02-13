package mcc_2016_g05_p2.niksula.hut.fi.rpc;

/* Never trust Google to actually deliver on spec implementations.
 * Everyhting has to be implemented in-house or you will be fucked over.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/* package */ class MinimalHTTPSClient extends IMinimalHTTPClient
{
	private SSLSocket m_socket;
	private InputStream m_input;
	private OutputStream m_output;
	private boolean m_httpPreambleRead = false;

	public void connect() throws IOException
	{
		TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(
							java.security.cert.X509Certificate[] certs, String authType) {
					}
				}
		};

		try
		{
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());

			m_socket = (SSLSocket)sc.getSocketFactory().createSocket(Host, Port);
			if (!m_socket.isConnected())
				throw new IOException("cannot connect");
			m_input = new BufferedInputStream(m_socket.getInputStream());
			m_output = new BufferedOutputStream(m_socket.getOutputStream());

			writeRequest();
			if (!DoOutput)
				readResponse();
			if (!DoOutput)
				m_output.close();
			if (!DoInput)
				m_input.close();
		}
		catch (NoSuchAlgorithmException|KeyManagementException|IOException ex)
		{
			try {
				if (m_socket != null)
					m_socket.close();
			} catch (IOException _) {
				// suppress
			}
			throw new IOException(ex);
		}
	}

	private void writeRequest() throws IOException
	{
		String request = String.format(Locale.ROOT,
				"%s /%s HTTP/1.1\r\n" +
				"Host: %s:%d\r\n" +
				"User-Agent: MiniHTTP/0.0\r\n" +
				"Connection: close\r\n",
				Method, Route, Host, Port);

		for (HashMap.Entry<String,String> field : ExtraHeaders.entrySet())
			request += String.format(Locale.ROOT, "%s: %s\r\n", field.getKey(), field.getValue());
		request += "\r\n";

		m_output.write(request.getBytes(StandardCharsets.UTF_8));
		m_output.flush();
	}

	private void readResponse() throws IOException
	{
		String firstLine = readLine();
		if (!firstLine.equals("HTTP/1.1 200 OK"))
			throw new IOException("Illegal response");

		boolean useChunkReader = false;
		for (;;)
		{
			String headerLine = readLine();
			if (headerLine.isEmpty())
				break;
			if (headerLine.startsWith("Content-Length: "))
				ResponseContentLength = Integer.parseInt(headerLine.substring("Content-Length: ".length()).trim());
			if (headerLine.equals("Transfer-Encoding: chunked"))
				useChunkReader = true;
		}

		if (useChunkReader)
			m_input = new ChunkedDecoderInputStream(m_input);
		m_httpPreambleRead = true;
	}

	private String readLine () throws IOException
	{
		// Never trust a "standard" libary, do everything manually
		StringBuilder result = new StringBuilder();
		byte chunk[] = new byte[128];
		boolean lastWasCR = false;
		int offset = 0;

		for (;;)
		{
			m_input.mark(chunk.length);

			for (;;)
			{
				int available = chunk.length - offset;
				int numRead = m_input.read(chunk, offset, available);
				if (numRead <= 0)
					throw new IOException("Unexpected EOF");

				// Scan for \r\n
				int eolPos = -1;
				if (lastWasCR && chunk[offset] == '\n')
					eolPos = offset + 1;
				if (eolPos == -1)
				{
					for (int i = offset; i < offset+numRead-1; ++i)
					{
						if (chunk[i] == '\r' && chunk[i+1] == '\n')
						{
							eolPos = i+2;
							break;
						}
					}
				}
				if (eolPos != -1)
				{
					if (eolPos > 2)
						result.append(new String(chunk, 0, eolPos-2, StandardCharsets.UTF_8));

					m_input.reset();
					long numSkipped = m_input.skip(eolPos);
					if (numSkipped != eolPos)
						throw new IOException("faielf to skip");
					return result.toString();
				}

				offset += numRead;
				lastWasCR = chunk[offset-1] == '\r';
				if (offset == chunk.length)
					break;
			}

			// Flush chunk
			lastWasCR = chunk[chunk.length-1] == '\r';
			result.append(new String(chunk, StandardCharsets.UTF_8));
			offset = 0;
		}
	}
	public void disconnect() throws IOException
	{
		if (m_socket != null)
			m_socket.close();
		m_socket = null;
	}

	public InputStream getInputStream() throws IOException
	{
		if (!DoInput)
			return null;
		if (!m_httpPreambleRead)
			readResponse();
		return m_input;
	}

	public OutputStream getOutputStream() throws IOException
	{
		if (!DoOutput)
			return null;
		return m_output;
	}
}
