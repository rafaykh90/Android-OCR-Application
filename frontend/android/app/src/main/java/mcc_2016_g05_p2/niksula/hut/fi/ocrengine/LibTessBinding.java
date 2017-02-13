package mcc_2016_g05_p2.niksula.hut.fi.ocrengine;

import java.lang.String;


/* package */ class LibTessBinding
{
	static
	{
		System.loadLibrary("tessbinding-native");
	}

	/* package */ static native long open (String dataDir);
	/* package */ static native void close (long handle);
	/* package */ static native String process (long handle, int pixelData[], int width, int height);
};
