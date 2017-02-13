package mcc_2016_g05_p2.niksula.hut.fi.ocrdriver;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import android.graphics.Bitmap;
import android.os.Bundle;

import mcc_2016_g05_p2.niksula.hut.fi.android.BenchmarkResultScreen;
import mcc_2016_g05_p2.niksula.hut.fi.asynctools.Promise;
import mcc_2016_g05_p2.niksula.hut.fi.ocrengine.TesseractEngine;
import mcc_2016_g05_p2.niksula.hut.fi.rpc.IRemoteRPC;


public class BenchmarkOCRDriver implements IOCRDriver
{
	private final IRemoteRPC m_rpc;

	public BenchmarkOCRDriver (IRemoteRPC rpc)
	{
		m_rpc = rpc;
	}

	public Future<Result> beginProcess (final Bitmap pages[], boolean invokedFromCamera)
	{
		final Promise<Result> promise = new Promise<>();
		final Runnable task = new Runnable()
		{
			@Override
			public void run ()
			{
				// Begin the OCR in the cloud
				Future<IRemoteRPC.UploadImagesResult> uploadFuture = m_rpc.beginUploadImages(pages);

				// Run OCR locally
				IRemoteRPC.UploadStatistics localStats = measureLocalOcr(pages);

				// Wait for results from the cloud
				final IRemoteRPC.UploadStatistics uploadStats;
				try
				{
					uploadStats = uploadFuture.get().stats;
				}
				catch (InterruptedException|CancellationException|ExecutionException ex)
				{
					// if upload is "interrupted", "cancelled" or an error happens during exec,
					// fail upload.
					promise.reject();
					return;
				}

				// Create the intent params
				Result result = new Result();
				result.resultActivityClass = BenchmarkResultScreen.class;
				result.extras = new Bundle();
				result.extras.putParcelable(BenchmarkResultScreen.EXTRA_REMOTE_STATS, uploadStats);
				result.extras.putParcelable(BenchmarkResultScreen.EXTRA_LOCAL_STATS, localStats);
				promise.resolve(result);
			}
		};

		// Fire and forget
		final Thread thread = new Thread(task);
		thread.start();
		return promise;
	}

	private IRemoteRPC.UploadStatistics measureLocalOcr (Bitmap pages[])
	{
		int numPages = pages.length;
		IRemoteRPC.UploadStatistics stats = new IRemoteRPC.UploadStatistics();
		stats.pages = new IRemoteRPC.UploadStatistics.Page[numPages];
		for (int ndx = 0; ndx < numPages; ++ndx)
		{
			IRemoteRPC.UploadStatistics.Page pageStats = stats.pages[ndx] = new IRemoteRPC.UploadStatistics.Page();
			pageStats.numPayloadBytes = 0;

			long startTime = System.nanoTime();
			TesseractEngine.process(pages[ndx]);
			long endTime = System.nanoTime();

			pageStats.numProcessingSeconds = (float)((endTime - startTime + 500_000_000) / 1_000_000_000.);
		}
		return stats;
	}
};
