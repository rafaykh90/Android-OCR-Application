package mcc_2016_g05_p2.niksula.hut.fi.ocrdriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Future;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;

import mcc_2016_g05_p2.niksula.hut.fi.android.IntentExtraStorage;
import mcc_2016_g05_p2.niksula.hut.fi.android.ShowOCRTextScreen;
import mcc_2016_g05_p2.niksula.hut.fi.asynctools.Promise;
import mcc_2016_g05_p2.niksula.hut.fi.ocrengine.TesseractEngine;


public class LocalOCRDriver implements IOCRDriver
{
	public Future<Result> beginProcess (final Bitmap pages[], final boolean invokedFromCamera)
	{
		final Promise<Result> promise = new Promise<>();
		final Runnable task = new Runnable()
		{
			@Override
			public void run ()
			{
				// Do the OCR locally
				String pageTexts[] = new String[pages.length];
				for (int ndx = 0; ndx < pages.length; ++ndx)
					pageTexts[ndx] = TesseractEngine.process(pages[ndx]);

				// Create the intent params
				Result result = new Result();
				result.resultActivityClass = ShowOCRTextScreen.class;
				result.extras = new Bundle();
				result.extras.putStringArray(ShowOCRTextScreen.EXTRA_OCR_TEXT, pageTexts);
				result.extras.putSerializable(ShowOCRTextScreen.EXTRA_OCR_DATE, new Date());
				result.extras.putInt(ShowOCRTextScreen.EXTRA_IMAGES_RKEY, IntentExtraStorage.storeObject(pages));
				result.extras.putBoolean(ShowOCRTextScreen.EXTRA_SHOW_RETAKE_IMAGE, invokedFromCamera);
				promise.resolve(result);
			}
		};

		// Fire and forget
		final Thread thread = new Thread(task);
		thread.start();
		return promise;
	}
};
