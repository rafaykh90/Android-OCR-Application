package mcc_2016_g05_p2.niksula.hut.fi.android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

import mcc_2016_g05_p2.niksula.hut.fi.rpc.IRemoteRPC;

public class BenchmarkResultScreen extends AppCompatActivity {
    public static final String EXTRA_REMOTE_STATS = "BenchmarkResultScreen.RemoteStats";
    public static final String EXTRA_LOCAL_STATS = "BenchmarkResultScreen.LocalStats";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benchmark_result_screen);

        IRemoteRPC.UploadStatistics remoteStats = getIntent().getParcelableExtra(EXTRA_REMOTE_STATS);
        IRemoteRPC.UploadStatistics localStats = getIntent().getParcelableExtra(EXTRA_LOCAL_STATS);

        // General
        {
            ((TextView) findViewById(R.id.text_numImages)).setText(Integer.toString(localStats.pages.length));
            int sumBytes = 0;
            for (IRemoteRPC.UploadStatistics.Page page : remoteStats.pages)
                sumBytes += page.numPayloadBytes;
            ((TextView) findViewById(R.id.text_transferred)).setText(formatNumBytes(sumBytes));
        }

        // Local
        {
            float samples[] = new float[localStats.pages.length];
            for (int ndx = 0; ndx < localStats.pages.length; ++ndx)
                samples[ndx] = (float)localStats.pages[ndx].numProcessingSeconds;

            Distribution dist = getDistribution(samples);
            ((TextView) findViewById(R.id.text_localMeanTime)).setText(formatNumSeconds(dist.mean));
            ((TextView) findViewById(R.id.text_localStdTime)).setText(formatNumSeconds(dist.std));
            ((TextView) findViewById(R.id.text_localMaxTime)).setText(formatNumSeconds(dist.max) + " in index " + dist.maxNdx);
            ((TextView) findViewById(R.id.text_localMinTime)).setText(formatNumSeconds(dist.min) + " in index " + dist.minNdx);
        }

        // Remote, time
        {
            float samples[] = new float[remoteStats.pages.length];
            for (int ndx = 0; ndx < remoteStats.pages.length; ++ndx)
                samples[ndx] = (float)remoteStats.pages[ndx].numProcessingSeconds;

            Distribution dist = getDistribution(samples);
            ((TextView) findViewById(R.id.text_remoteMeanTime)).setText(formatNumSeconds(dist.mean));
            ((TextView) findViewById(R.id.text_remoteStdTime)).setText(formatNumSeconds(dist.std));
            ((TextView) findViewById(R.id.text_remoteMaxTime)).setText(formatNumSeconds(dist.max) + " in index " + dist.maxNdx);
            ((TextView) findViewById(R.id.text_remoteMinTime)).setText(formatNumSeconds(dist.min) + " in index " + dist.minNdx);
        }

        // Remote, data
        {
            float samples[] = new float[remoteStats.pages.length];
            for (int ndx = 0; ndx < remoteStats.pages.length; ++ndx)
                samples[ndx] = (float)remoteStats.pages[ndx].numPayloadBytes;

            Distribution dist = getDistribution(samples);
            ((TextView) findViewById(R.id.text_remoteMeanData)).setText(formatNumBytes(dist.mean));
            ((TextView) findViewById(R.id.text_remoteStdData)).setText(formatNumBytes(dist.std));
            ((TextView) findViewById(R.id.text_remoteMaxData)).setText(formatNumBytes((int)dist.max) + " in index " + dist.maxNdx);
            ((TextView) findViewById(R.id.text_remoteMinData)).setText(formatNumBytes((int)dist.min) + " in index " + dist.minNdx);
        }
    }

    private static class Distribution
    {
        public float mean, std, min, max;
        public int minNdx, maxNdx;
    }
    private static Distribution getDistribution (float samples[])
    {
        Distribution d = new Distribution();

        // Mean
        float sum = 0.0f;
        for (float sample : samples)
            sum += sample;
        d.mean = sum / samples.length;

        // Std
        float varianceN = 0.0f;
        for (float sample : samples)
            varianceN += (sample - d.mean) * (sample - d.mean);
        d.std = (float)Math.sqrt(varianceN / samples.length);

        // Min max
        d.min = samples[0];
        d.max = samples[0];
        d.minNdx = 0;
        d.maxNdx = 0;
        for (int ndx = 0; ndx < samples.length; ++ndx)
        {
            if (samples[ndx] > d.max)
            {
                d.max = samples[ndx];
                d.maxNdx = ndx;
            }
            if (samples[ndx] < d.min)
            {
                d.min = samples[ndx];
                d.minNdx = ndx;
            }
        }
        return d;
    }

    private static String formatNumBytes (int numBytes)
    {
        if (numBytes < 1024)
            return Integer.toString(numBytes) + "B";
        else if (numBytes < 1024*1024)
            return Integer.toString(numBytes/1024) + "kB";
        else
            return Integer.toString(numBytes/1024/1024) + "MB";
    }
    private static String formatNumBytes (float numBytes)
    {
        if (numBytes < 1024.)
            return String.format(Locale.ROOT, "%.2fB", numBytes);
        else if (numBytes < 1024*1024)
            return String.format(Locale.ROOT, "%.2fkB", numBytes/1024);
        else
            return String.format(Locale.ROOT, "%.2fMB", numBytes/1024/1024);
    }
    private static String formatNumSeconds (float numSeconds)
    {
        return String.format(Locale.ROOT, "%.1fms", numSeconds * 1000);
    }
}
