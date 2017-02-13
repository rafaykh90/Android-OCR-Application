package mcc_2016_g05_p2.niksula.hut.fi.rpc;

import android.graphics.Bitmap;


/* package */ class ThumbUtils
{
    static /* package */ Bitmap speculateThumb (Bitmap src)
    {
        int thumbTargetSize = 128;
        int newWidth = src.getWidth();
        int newHeight = src.getHeight();

        if (newWidth > thumbTargetSize)
        {
            newHeight = Math.max(1, (int)Math.round((newHeight * (thumbTargetSize / (float)newWidth))));
            newWidth = thumbTargetSize;
        }
        if (newHeight > thumbTargetSize)
        {
            newWidth = Math.max(1, (int)Math.round(newWidth * (thumbTargetSize / (float)newHeight)));
            newHeight = thumbTargetSize;
        }

        return Bitmap.createScaledBitmap(src, newWidth, newHeight, true);
    }
}
