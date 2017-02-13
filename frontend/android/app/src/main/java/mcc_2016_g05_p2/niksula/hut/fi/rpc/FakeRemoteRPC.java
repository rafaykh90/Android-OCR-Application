package mcc_2016_g05_p2.niksula.hut.fi.rpc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;

import android.graphics.Bitmap;

import mcc_2016_g05_p2.niksula.hut.fi.asynctools.Promise;


public class FakeRemoteRPC implements IRemoteRPC
{
	private final ArrayList<Record> m_historyRecords;
	private final HashMap<String, Bitmap> m_userImages;
	private static int s_nextToken = 0;

	public FakeRemoteRPC ()
	{
		m_historyRecords = new ArrayList<>();
		m_userImages = new HashMap<>();

		for (int i = 0; i < 5; ++i)
			appendFakeRecord();
	}
	private void appendFakeRecord()
	{
		//m_historyRecords.add(new Record(){{  token = "" + s_nextToken++; }});

		Random pageRng = new Random(12345*s_nextToken);

		int numPages = 1;
		if (pageRng.nextFloat() > 0.7)
			numPages = 2 + (int)(pageRng.nextFloat() * 5);

		Record fakeRecord = new Record();
		fakeRecord.pages = new Record.Page[numPages];
		for (int ndx = 0; ndx < fakeRecord.pages.length; ++ndx)
		{
			Record.Page page = fakeRecord.pages[ndx] = new Record.Page();
			int a = 255;
			int r = 100 + (int)(pageRng.nextFloat() * 155);
			int g = ((int)(128 + 255 * ( ndx / (float)fakeRecord.pages.length))) % 256;
			int b = (int)(255 * ( ndx / (float)fakeRecord.pages.length));
			int pixel[] = { (a << 24) | (r << 16) | (g << 8) | b };
			page.thumb = Bitmap.createBitmap(pixel, 1, 1, Bitmap.Config.ARGB_8888);
			page.text = String.format("translated text of page %d", ndx);
			page.imageId = "" + s_nextToken++;
		}

		fakeRecord.date = new Date((new Date().getTime()) - (long)(pageRng.nextFloat() * 1000000));
		m_historyRecords.add(fakeRecord);
	}

	public List<Record> getHistory ()
	{
		return m_historyRecords;
	}
	public Future<Bitmap> beginResolveImage (final String imageId)
	{
		// Run and resolve record in some time in the future... maybe?
		final Promise<Bitmap> promise = new Promise<>();
		final Runnable task = new Runnable()
		{
			@Override
			public void run ()
			{
				Random rng = new Random();

				try
				{
					long resolveTime = (long)(rng.nextFloat() * 4000);
					Thread.sleep(resolveTime);
				}
				catch (InterruptedException ex)
				{
					// ignore
					Thread.currentThread().interrupt();
				}

				if (rng.nextFloat() > 0.9)
				{
					promise.reject();
					return;
				}

				// Just uploaded, keep content for sanity
				synchronized (m_userImages)
				{
					if (m_userImages.containsKey(imageId))
					{
						promise.resolve(m_userImages.get(imageId));
						return;
					}
				}

				// Serve from """Remote"""", generate random image

				Random pageRng = new Random(imageId.hashCode());
				Bitmap bitmap;

				int a = 255;
				int r = 100 + (int)(pageRng.nextFloat() * 155);
				int g = ((int)(128 + 255 * pageRng.nextFloat())) % 256;
				int b = (int)(255 * pageRng.nextFloat());
				int pixel[] = { (a << 24) | (r << 16) | (g << 8) | b };
				bitmap = Bitmap.createBitmap(pixel, 1, 1, Bitmap.Config.ARGB_8888);

				promise.resolve(bitmap);
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
		final Runnable task = new Runnable()
		{
			@Override
			public void run ()
			{
				Random rng = new Random();

				try
				{
					long resolveTime = (long)(rng.nextFloat() * 4000);
					Thread.sleep(resolveTime);
				}
				catch (InterruptedException ex)
				{
					// ignore
					Thread.currentThread().interrupt();
				}

				if (rng.nextFloat() > 0.9)
				{
					promise.reject();
					return;
				}

				int numPages = pages.length;

				Record fakeRecord = new Record();
				fakeRecord.pages = new Record.Page[numPages];
				for (int ndx = 0; ndx < fakeRecord.pages.length; ++ndx)
				{
					Record.Page page = fakeRecord.pages[ndx] = new Record.Page();
					page.thumb = ThumbUtils.speculateThumb(pages[ndx]);
					page.text = "FakeRemoteRPC OCR'd text, page " + ndx;
					page.imageId = "" + s_nextToken++;
				}
				fakeRecord.date = new Date();

				UploadStatistics fakeStats = new UploadStatistics();
				fakeStats.pages = new UploadStatistics.Page[numPages];
				for (int ndx = 0; ndx < fakeRecord.pages.length; ++ndx)
				{
					UploadStatistics.Page pageStats = fakeStats.pages[ndx] = new UploadStatistics.Page();
					pageStats.numPayloadBytes = (int)(rng.nextFloat() * 5_000_000);
					pageStats.numProcessingSeconds = rng.nextFloat();
				}

				UploadImagesResult result = new UploadImagesResult();
				result.record = fakeRecord;
				result.stats = fakeStats;

				m_historyRecords.add(0, fakeRecord);
				synchronized (m_userImages)
				{
					for (int ndx = 0; ndx < numPages; ++ndx)
						m_userImages.put(fakeRecord.pages[ndx].imageId, pages[ndx]);
				}
				promise.resolve(result);
			}
		};

		// Fire and forget
		final Thread thread = new Thread(task);
		thread.start();
		return promise;
	}
};
