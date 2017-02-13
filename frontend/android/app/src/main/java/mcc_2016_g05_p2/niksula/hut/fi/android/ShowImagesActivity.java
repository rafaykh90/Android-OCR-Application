package mcc_2016_g05_p2.niksula.hut.fi.android;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ShowImagesActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGES_RKEY = "ShowImagesActivity.Images";

    private Bitmap[] m_images;
    private int m_curIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_images);

        int rkeyImages = getIntent().getIntExtra(EXTRA_IMAGES_RKEY, -1);
        m_images = (Bitmap[])IntentExtraStorage.tryRetrieve(rkeyImages);
        if (m_images == null)
            m_images = new Bitmap[0];

        m_curIndex = 0;
        updateUI();
    }

    private void updateUI ()
    {
        if (m_curIndex < 0 || m_curIndex >= m_images.length)
            return;

        ((TextView)findViewById(R.id.text_imgPos)).setText(Integer.toString(m_curIndex+1) + " / " + Integer.toString(m_images.length));
        ((ImageView)findViewById(R.id.image_showImage)).setImageBitmap(m_images[m_curIndex]);
    }
    public void onClickPrecImage (View view)
    {
        if (m_curIndex > 0)
        {
            --m_curIndex;
            updateUI();
        }
    }
    public void onClickSuccImage (View view)
    {
        if (m_curIndex < m_images.length - 1)
        {
            ++m_curIndex;
            updateUI();
        }
    }
}
