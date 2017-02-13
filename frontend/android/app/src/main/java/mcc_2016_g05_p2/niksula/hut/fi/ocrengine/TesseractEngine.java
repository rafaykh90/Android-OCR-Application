package mcc_2016_g05_p2.niksula.hut.fi.ocrengine;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class TesseractEngine
{
	private static Object s_prepareLock = new Object();
	private static boolean s_prepared = false;
	private static AssetManager s_assetManager;
	private static String s_trainingDataDirectory;

	public static String process (Bitmap image)
	{
		prepare();

		int pixelData[] = new int[image.getWidth() * image.getHeight()];
		image.getPixels(pixelData, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

		long handle = LibTessBinding.open(getTrainingDataDirectory());
		if (handle == 0)
			return "<Internal Error: cannot start OCR engine>";

		String recognizedText = LibTessBinding.process(handle, pixelData, image.getWidth(), image.getHeight());
		LibTessBinding.close(handle);

		if (recognizedText == null)
			return "<Internal Error: recognition error>";
		return recognizedText;
	}

	private static void prepare ()
	{
		if (s_prepared)
			return;
		synchronized (s_prepareLock)
		{
			if (s_prepared)
				return;

			copyTessDataToWorldReadable();
			s_prepared = true;
		}
	}

	private static void copyTessDataToWorldReadable ()
	{
		byte buffer[] = new byte[1024];

		try
		{
			new File(getTrainingDataDirectory() + "tessdata/").mkdirs();

			String[] tessFiles = s_assetManager.list("tessdata");
			for (String tessFile : tessFiles)
			{
				String tessPath = "tessdata/" + tessFile;
				InputStream tessIStream = s_assetManager.open(tessPath);
				OutputStream tessOStream = new FileOutputStream(getTrainingDataDirectory() + "tessdata/" + tessFile);
				for (;;)
				{
					int numRead = tessIStream.read(buffer);
					if (numRead == -1)
						break;
					tessOStream.write(buffer, 0, numRead);
				}
				tessIStream.close();
				tessOStream.close();
			}
		}
		catch (IOException ex)
		{
			// cant really do anythign
			Log.d("OCREngine", "Failed to prepare data", ex);
		}
	}

	private static String getTrainingDataDirectory ()
	{
		return s_trainingDataDirectory;
	}

	public static void injectConfig (AssetManager manager, String appDataDir)
	{
		s_assetManager = manager;
		s_trainingDataDirectory = appDataDir + "/";
	}
};
