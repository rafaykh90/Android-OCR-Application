package mcc_2016_g05_p2.niksula.hut.fi.rpc;

import java.util.Date;
import java.util.concurrent.Future;
import java.util.List;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;


/**
 * Remote Remote Procedure Call...
 */
public interface IRemoteRPC
{
	public static class Record
	{
		public static class Page
		{
			public Bitmap thumb;
			public String text;
			public String imageId;
		}
		public Page pages[];
		public Date date;
	};
	public static class UploadStatistics implements Parcelable
	{
		public static class Page
		{
			public float	numProcessingSeconds;
			public int		numPayloadBytes;
		};
		public Page pages[];

		// Parcelable
		public int describeContents()
		{
			return 0;
		}
		public void writeToParcel (Parcel dest, int flags)
		{
			dest.writeInt(pages.length);
			for (int ndx = 0; ndx < pages.length; ++ndx)
			{
				dest.writeFloat(pages[ndx].numProcessingSeconds);
				dest.writeInt(pages[ndx].numPayloadBytes);
			}
		}
		public static final Parcelable.Creator<UploadStatistics> CREATOR
				= new Parcelable.Creator<UploadStatistics>()
		{
			public UploadStatistics createFromParcel (Parcel parcel)
			{
				UploadStatistics stats = new UploadStatistics();
				stats.pages = new Page[parcel.readInt()];
				for (int ndx = 0; ndx < stats.pages.length; ++ndx)
				{
					Page page = stats.pages[ndx] = new Page();
					page.numProcessingSeconds = parcel.readFloat();
					page.numPayloadBytes = parcel.readInt();
				}
				return stats;
			}
			public UploadStatistics[] newArray (int size)
			{
				return new UploadStatistics[size];
			}
		};
	};
	public static class UploadImagesResult
	{
		public Record record;
		public UploadStatistics stats;
	};

	public List<Record>					getHistory			();
	public Future<Bitmap>				beginResolveImage	(String imageId);
	public Future<UploadImagesResult>	beginUploadImages	(Bitmap pages[]);
}
