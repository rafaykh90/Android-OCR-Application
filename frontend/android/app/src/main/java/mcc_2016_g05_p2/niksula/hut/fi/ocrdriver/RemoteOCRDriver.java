package mcc_2016_g05_p2.niksula.hut.fi.ocrdriver;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.graphics.Bitmap;
import android.os.Bundle;

import mcc_2016_g05_p2.niksula.hut.fi.android.IntentExtraStorage;
import mcc_2016_g05_p2.niksula.hut.fi.android.ShowOCRTextScreen;
import mcc_2016_g05_p2.niksula.hut.fi.asynctools.Promise;
import mcc_2016_g05_p2.niksula.hut.fi.rpc.IRemoteRPC;


public class RemoteOCRDriver implements IOCRDriver
{
	private final IRemoteRPC m_rpc;

	public RemoteOCRDriver (IRemoteRPC rpc)
	{
		m_rpc = rpc;
	}

	public Future<Result> beginProcess (final Bitmap pages[], final boolean invokedFromCamera)
	{
		final Promise<Result> promise = new Promise<>();
		final Runnable task = new Runnable()
		{
			@Override
			public void run ()
			{
				// Do the OCR in the cloud
				Future<IRemoteRPC.UploadImagesResult> uploadFuture = m_rpc.beginUploadImages(pages);
				final IRemoteRPC.UploadImagesResult upload;
				try
				{
					upload = uploadFuture.get();
				}
				catch (InterruptedException|CancellationException|ExecutionException ex)
				{
					// if upload is "interrupted", "cancelled" or an error happens during exec,
					// fail upload.
					promise.reject();
					return;
				}

				String pageTexts[] = new String[upload.record.pages.length];
				for (int ndx = 0; ndx < upload.record.pages.length; ++ndx)
					pageTexts[ndx] = upload.record.pages[ndx].text;

				// Create the intent params
				Result result = new Result();
				result.resultActivityClass = ShowOCRTextScreen.class;
				result.extras = new Bundle();
				result.extras.putStringArray(ShowOCRTextScreen.EXTRA_OCR_TEXT, pageTexts);
				result.extras.putSerializable(ShowOCRTextScreen.EXTRA_OCR_DATE, upload.record.date);
				result.extras.putInt(ShowOCRTextScreen.EXTRA_IMAGES_RKEY, IntentExtraStorage.storeObject(pages));
				result.extras.putBoolean(ShowOCRTextScreen.EXTRA_SHOW_RETAKE_IMAGE, false);
				promise.resolve(result);
			}
		};

		// Fire and forget
		final Thread thread = new Thread(task);
		thread.start();
		return promise;
	}
};
