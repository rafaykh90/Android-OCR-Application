package mcc_2016_g05_p2.niksula.hut.fi.ocrdriver;

import java.util.concurrent.Future;

import android.graphics.Bitmap;
import android.os.Bundle;


public interface IOCRDriver
{
	public static class Result
	{
		// \note: totally not a hack and bad design :^)

		public Class<?>	resultActivityClass;	//!< activity class that will show the result screen
		public Bundle	extras;					//!< extras for the activity intent
	};

	public Future<Result> beginProcess (Bitmap pages[], boolean invokedFromCamera);
};
