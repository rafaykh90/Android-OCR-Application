package mcc_2016_g05_p2.niksula.hut.fi.rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Base64InputStream;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import mcc_2016_g05_p2.niksula.hut.fi.android.LoginScreen;
import mcc_2016_g05_p2.niksula.hut.fi.asynctools.Promise;

import static mcc_2016_g05_p2.niksula.hut.fi.android.Constants.SERVER_HOST;
import static mcc_2016_g05_p2.niksula.hut.fi.android.Constants.SERVER_PORT;


public class RemoteRPC implements IRemoteRPC
{
	public static class OpenResult
	{
		public IRemoteRPC rpc; //!< null on error
		public String errorString; //!< null on success
	}
	private ArrayList<Record> m_historyRecords;

	private static IMinimalHTTPClient prepareConnection (String route, String oAuthToken)
	{
		IMinimalHTTPClient conn = new MinimalHTTPSClient();
		conn.Host = SERVER_HOST;
		conn.Port = SERVER_PORT;
		conn.Route = route;
		conn.ExtraHeaders.put("id_token", oAuthToken);
		return conn;
	}

	private static String readStream (InputStream stream, int len) throws IOException, UnsupportedEncodingException {
		Reader reader = new InputStreamReader(stream, "UTF-8");
		char[] buffer = new char[len];

		int remaining = len;
		int offset = 0;

		while (remaining != 0)
		{
			int numRead = reader.read(buffer, offset, remaining);
			if (numRead == -1)
			{
				throw new IOException("early EOF");
			}
			offset += numRead;
			remaining -= numRead;
		}
		return new String(buffer);
	}

	private static void writePostRequest (IMinimalHTTPClient conn) throws IOException
	{
		conn.Method = "POST";
		//conn.ExtraHeaders.put("Content-Type", "application/json");
		conn.DoInput = true;
		conn.connect();
	}

	private static void writeGetRequest (IMinimalHTTPClient conn) throws IOException
	{
		conn.Method = "GET";
		conn.DoInput = true;
		conn.connect();
	}

	private static class HTTPMultipartPart
	{
		String name;
		String path;
		byte data[];

		private byte[] getHeader ()
		{
			// nice escaping :^)
			String header = "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + path + "\"\r\n"
					+ "Content-Type: application/octet-stream\r\n\r\n";
			return header.getBytes(StandardCharsets.UTF_8);
		}
	}
	private static boolean arrayContains (byte haystack[], byte needle[])
	{
		// "high" quality code
		search: for (int startNdx = 0; startNdx <= haystack.length - needle.length; ++startNdx)
		{
			for (int needleNdx = 0; needleNdx < needle.length; ++needleNdx)
			{
				if (needle[needleNdx] != haystack[startNdx + needleNdx])
					continue search;
			}
			return true;
		}
		return false;
	}
	private static String getMultipartBoundaryString (HTTPMultipartPart parts[])
	{
		// even more "high" quality code
		String candidate = "----------XXXbound";
		for (;;)
		{
			byte candidateBytes[] = candidate.getBytes(StandardCharsets.UTF_8);
			boolean badBoundary = false;

			for (HTTPMultipartPart part : parts)
			{
				if (arrayContains(part.data, candidateBytes))
				{
					badBoundary = true;
					break;
				}
			}

			if (!badBoundary)
				return candidate;

			candidate += "-123123";
		}
	}
	private static void writeMultipartRequest (IMinimalHTTPClient conn, HTTPMultipartPart parts[]) throws IOException
	{
		String boundaryString = getMultipartBoundaryString(parts);
		byte boundaryBytes[] = boundaryString.getBytes(StandardCharsets.UTF_8);
		byte crlf[] = {'\r', '\n'};
		byte doubleHyphen[] = {'-', '-'};

		int requestLength = 0;
		for (int partNdx = 0; partNdx < parts.length; ++partNdx)
		{
			requestLength += doubleHyphen.length;
			requestLength += boundaryBytes.length;
			requestLength += crlf.length;
			requestLength += parts[partNdx].getHeader().length;
			requestLength += parts[partNdx].data.length;
			requestLength += crlf.length;
		}
		requestLength += doubleHyphen.length;
		requestLength += boundaryBytes.length;
		requestLength += doubleHyphen.length;
		requestLength += crlf.length;

		// Setup conn
		conn.Method = "POST";
		conn.ExtraHeaders.put("Content-Type", "multipart/form-data; boundary=" + boundaryString);
		conn.ExtraHeaders.put("Content-Length", Integer.toString(requestLength));
		conn.DoInput = true;
		conn.DoOutput = true;
		conn.connect();

		OutputStream os = conn.getOutputStream();
		for (int partNdx = 0; partNdx < parts.length; ++partNdx)
		{
			os.write(doubleHyphen);
			os.write(boundaryBytes);
			os.write(crlf);
			os.write(parts[partNdx].getHeader());
			os.flush();

			// Write data in chunks. Otherwise Socket shits itself and fails
			// Thanks Android!
			final int chunkSize = 1024;
			for (int offset = 0; offset < parts[partNdx].data.length; offset += chunkSize)
			{
				byte fullData[] = parts[partNdx].data;
				os.write(fullData, offset, Math.min(chunkSize, fullData.length - offset));
				os.flush();
			}
			os.write(crlf);
		}
		os.write(doubleHyphen);
		os.write(boundaryBytes);
		os.write(doubleHyphen);
		os.write(crlf);
		os.flush();
		os.close();
	}

	private static String getHashString (byte[] bytes)
	{
		// \note: Pasta from group project 1
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.reset();
			digest.update(bytes);

			byte[] a = digest.digest();
			int len = a.length;
			StringBuilder sb = new StringBuilder(len << 1);
			for (int i = 0; i < len; i++) {
				sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
				sb.append(Character.forDigit(a[i] & 0x0f, 16));
			}
			return sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			// Never happens.
			return "MessageDigest_does_not_conform_to_the_Java_specification";
		}

	}
	private static void writeUploadRequest (IMinimalHTTPClient conn, byte[][] slugs) throws IOException
	{
		HTTPMultipartPart parts[] = new HTTPMultipartPart[slugs.length];
		for (int slugNdx = 0; slugNdx < slugs.length; ++slugNdx)
		{
			parts[slugNdx] = new HTTPMultipartPart();
			parts[slugNdx].name = "file" + Integer.toString(1 + slugNdx);
			parts[slugNdx].path = "pic_" + getHashString(slugs[slugNdx]) + ".jpg";
			parts[slugNdx].data = slugs[slugNdx];
		}
		writeMultipartRequest(conn, parts);
	}

	// Workaround for Sting(byte[], off, size, charset) effectively hanging with "large" buffers
	// Note: this workaround will decode incorrect values if multibyte sequence is at a chunk
	//       boundary.
	private static String bytesToUTF8String (byte bytes[])
	{
		int chunkSize = 1024;
		StringBuilder builder = new StringBuilder(bytes.length);
		for (int offset = 0; offset < bytes.length; offset += chunkSize)
		{
			int length = Math.min(chunkSize, bytes.length - offset);
			builder.append(new String(bytes, offset, length, StandardCharsets.UTF_8));
		}
		return builder.toString();
	}

	private static JSONObject readJsonResponse (IMinimalHTTPClient conn) throws IOException
	{
		byte[] bytes = readBytesResponse(conn);
		String resultString = bytesToUTF8String(bytes);

		try
		{
			return new JSONObject(resultString);
		}
		catch (JSONException ex)
		{
			throw new IOException(ex);
		}
	}

	private static byte[] readBytesResponse (IMinimalHTTPClient conn) throws IOException
	{
		InputStream is = conn.getInputStream();
		int contentLength = conn.ResponseContentLength;

		try
		{
			if (contentLength != -1)
			{
				byte[] buffer = new byte[contentLength];

				int remaining = contentLength;
				int offset = 0;

				while (remaining != 0)
				{
					int numRead = is.read(buffer, offset, remaining);
					if (numRead == -1) {
						throw new IOException("early EOF");
					}
					offset += numRead;
					remaining -= numRead;
				}
				return buffer;
			}
			else
			{
				// Read blindly till stream end
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				byte chunk[] = new byte[128];
				for (;;)
				{
					int numRead = is.read(chunk);
					if (numRead == -1)
						break;
					buffer.write(chunk, 0, numRead);
				}
				if (buffer.size()==0)
					throw new IOException("no data");
				return buffer.toByteArray();
			}
		}
		finally {
			try {
				is.close();
			} catch (IOException ex) {
				// suppress
			}
		}
	}

	private static Bitmap tryDecodeB64Image (String bitmapString)
	{
		Base64InputStream is = new Base64InputStream(new ByteArrayInputStream(bitmapString.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
		return BitmapFactory.decodeStream(is);
	}

	private static Bitmap tryDecodeBufferImage (String bitmapString)
	{
		try {
			JSONObject buffer = new JSONObject(bitmapString);
			if (!"Buffer".equals(buffer.getString("type")))
				return null;

			JSONArray array = buffer.getJSONArray("data");
			byte bytes[] = new byte[array.length()];
			for (int i = 0; i < array.length(); ++i)
				bytes[i] = (byte) array.getInt(i);

			ByteArrayInputStream is = new ByteArrayInputStream(bytes);
			return BitmapFactory.decodeStream(is);
		} catch (JSONException ex) {
			// suppress
			return null;
		}
	}

	private static Bitmap decodeImage (String bitmapString) throws IOException
	{
		Bitmap bitmap;

		bitmap = tryDecodeB64Image(bitmapString);
		if (bitmap != null)
			return bitmap;

		bitmap = tryDecodeBufferImage(bitmapString);
		if (bitmap != null)
			return bitmap;

		throw new IOException("illegal thumb data");
	}

	private static byte[] encodeImage (Bitmap image) throws IOException
	{
		int qp = 80;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		if (!image.compress(Bitmap.CompressFormat.JPEG, qp, os))
			throw new IOException("encode error");
		return os.toByteArray();
	}

	private static Record parseRecord (JSONObject jsonRecord) throws JSONException, IOException
	{
		JSONArray imgThumbs = jsonRecord.getJSONArray("imgThumbs");
		JSONArray imgNames = jsonRecord.getJSONArray("imgNames");
		JSONArray imgText = jsonRecord.getJSONArray("imgText");
		int numPages = imgText.length();
		Record record = new Record();

		record.pages = new Record.Page[numPages];
		for (int pageNdx = 0; pageNdx < numPages; ++pageNdx)
		{
			Record.Page page = record.pages[pageNdx] = new Record.Page();

			page.text = imgText.getString(pageNdx);
			page.thumb = decodeImage(imgThumbs.getString(pageNdx));
			page.imageId = imgNames.getString(pageNdx);
		}

		String dateField = jsonRecord.getString("createdAt");
		try
		{
			SimpleDateFormat utcInstantIsoFormat = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSS'Z'"); // 2016-12-06T09:25:39.297Z
			utcInstantIsoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			record.date = utcInstantIsoFormat.parse(dateField);
		}
		catch (ParseException ex)
		{
			throw new JSONException("Malformed date: " + dateField);
		}
		return record;
	}

	private static Future<String> getOAuthToken ()
	{
		final Promise<String> promise = new Promise<>();
		Runnable task = new Runnable() {
			@Override
			public void run() {
				GoogleApiClient client = LoginScreen.mGoogleApiClient;
				if (client == null)
				{
					promise.reject();
					return;
				}

				OptionalPendingResult<GoogleSignInResult> pendingResult = Auth.GoogleSignInApi.silentSignIn(client);
				GoogleSignInResult result = pendingResult.await();
				GoogleSignInAccount account = result.getSignInAccount();
				if (account == null)
				{
					promise.reject();
					return;
				}

				String token = account.getIdToken();
				if (token == null)
				{
					promise.reject();
					return;
				}
				promise.resolve(token);
			}
		};

		// Fire and forget
		final Thread thread = new Thread(task);
		thread.start();
		return promise;
	}
	public static Future<OpenResult> beginOpen ()
	{
		final Promise<OpenResult> promise = new Promise<>();
		Runnable task = new Runnable() {
			@Override
			public void run() {
				String oAuthId;
				try
				{
					oAuthId = getOAuthToken().get();
				}
				catch (InterruptedException|CancellationException|ExecutionException ex)
				{
					OpenResult result = new OpenResult();
					result.errorString = "no OAuth token";
					promise.resolve(result);
					return;
				}

				IMinimalHTTPClient conn = prepareConnection("history", oAuthId);
				JSONObject loginResponse;

				try
				{
					writePostRequest(conn);
					loginResponse = readJsonResponse(conn);
				}
				catch (IOException ex)
				{
					OpenResult result = new OpenResult();
					result.errorString = ex.toString();
					promise.resolve(result);
					return;
				}
				finally
				{
					try {
						conn.disconnect();
					} catch (IOException ex) {
						// suppress
					}
				}

				if (!loginResponse.has("message"))
				{
					OpenResult result = new OpenResult();
					result.errorString = "Server returned: " + loginResponse.toString();
					promise.resolve(result);
					return;
				}

				ArrayList<Record> records = new ArrayList<>();
				try
				{
					JSONArray history = loginResponse.getJSONArray("message");
					records.ensureCapacity(history.length());
					for (int ndx = 0; ndx < history.length(); ++ndx) {
						JSONObject historyRecord = history.getJSONObject(ndx);
						Record record = parseRecord(historyRecord);
						records.add(record);
					}
				}
				catch (JSONException|IOException ex)
				{
					OpenResult result = new OpenResult();
					result.errorString = "Malformed response: " + ex.toString();
					promise.resolve(result);
					return;
				}

				// Done
				RemoteRPC rpc = new RemoteRPC();
				rpc.m_historyRecords = records;

				OpenResult result = new OpenResult();
				result.rpc = rpc;
				promise.resolve(result);
				return;
			}
		};

		// Fire and forget
		final Thread thread = new Thread(task);
		thread.start();
		return promise;
	}

	public List<Record> getHistory()
	{
		synchronized (m_historyRecords)
		{
			return new ArrayList<>(m_historyRecords);
		}
	}
	public Future<Bitmap> beginResolveImage (final String imageId)
	{
		final Promise<Bitmap> promise = new Promise<>();
		Runnable task = new Runnable() {
			@Override
			public void run() {
				String oAuthId;
				try
				{
					oAuthId = getOAuthToken().get();
				}
				catch (InterruptedException|CancellationException|ExecutionException ex)
				{
					promise.reject();
					return;
				}

				IMinimalHTTPClient conn = prepareConnection("image/" + imageId, oAuthId);
				JSONObject imageResult;

				try
				{
					writeGetRequest(conn);
					imageResult = readJsonResponse(conn);
				}
				catch (IOException ex)
				{
					promise.reject();
					return;
				}
				finally
				{
					try {
						conn.disconnect();
					} catch (IOException ex) {
						// suppress
					}
				}

				// Read image
				Bitmap bitmap = null;
				try
				{
					bitmap = decodeImage(imageResult.getString("message"));
				}
				catch (IOException|JSONException ex)
				{
					promise.reject();
					return;
				}

				if (bitmap == null)
				{
					promise.reject();
					return;
				}

				promise.resolve(bitmap);
				return;
			}
		};

		// Fire and forget
		final Thread thread = new Thread(task);
		thread.start();
		return promise;
	}
	public Future<UploadImagesResult> beginUploadImages (final Bitmap pages[])
	{
		final Promise<UploadImagesResult> promise = new Promise<>();
		Runnable task = new Runnable() {
			@Override
			public void run() {
				String oAuthId;
				try
				{
					oAuthId = getOAuthToken().get();
				}
				catch (InterruptedException|CancellationException|ExecutionException ex)
				{
					promise.reject();
					return;
				}

				IMinimalHTTPClient conn = prepareConnection("ocr", oAuthId);
				UploadStatistics stats = new UploadStatistics();
				JSONObject uploadResponse;

				stats.pages = new UploadStatistics.Page[pages.length];
				for (int page = 0; page < pages.length; ++page)
					stats.pages[page] = new UploadStatistics.Page();

				// Upload
				try
				{
					// Create upload slugs
					byte pageData[][] = new byte[pages.length][];
					for (int page = 0; page < pages.length; ++page)
					{
						pageData[page] = encodeImage(pages[page]);
						stats.pages[page].numPayloadBytes = pageData[page].length;
					}

					// Write
					writeUploadRequest(conn, pageData);

					// Read
					uploadResponse = readJsonResponse(conn);
				}
				catch (IOException ex)
				{
					promise.reject();
					return;
				}
				finally
				{
					try {
						conn.disconnect();
					} catch (IOException ex) {
						// suppress
					}
				}

				Record record;
				try
				{
					JSONObject message = uploadResponse.getJSONObject("message");
					JSONObject statistics = uploadResponse.getJSONObject("imageStatistics");
					JSONArray imageTimes = statistics.getJSONArray("ocrTimesInSeconds");

					record = parseRecord(message);
					if (imageTimes.length() != pages.length)
						throw new IOException("invalid format");
					for (int page = 0; page < pages.length; ++page)
						stats.pages[page].numProcessingSeconds = (float)imageTimes.getDouble(page);
				}
				catch (JSONException|IOException ex)
				{
					promise.reject();
					return;
				}

				synchronized (m_historyRecords)
				{
					m_historyRecords.add(record);
				}

				UploadImagesResult result = new UploadImagesResult();
				result.record = record;
				result.stats = stats;
				promise.resolve(result);
				return;
			}
		};

		// Fire and forget
		final Thread thread = new Thread(task);
		thread.start();
		return promise;
	}
};
