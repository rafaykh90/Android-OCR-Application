package mcc_2016_g05_p2.niksula.hut.fi.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ShowOCRTextScreen extends AppCompatActivity {
    public static final String EXTRA_OCR_TEXT = "ShowOCRTextScreen.OrcText";
    public static final String EXTRA_OCR_DATE = "ShowOCRTextScreen.OrcDate";
    public static final String EXTRA_IMAGES_RKEY = "ShowOCRTextScreen.Images";
    public static final String EXTRA_SHOW_RETAKE_IMAGE = "ShowOCRTextScreen.ShowRetakeImage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_ocrtext_screen);

        // OCR text and date
        ((TextView)findViewById(R.id.text_ocrText)).setText(formatPageText(getIntent().getStringArrayExtra(EXTRA_OCR_TEXT)));
        ((TextView)findViewById(R.id.text_ocrDate)).setText(formatDate((Date)getIntent().getSerializableExtra(EXTRA_OCR_DATE)));

        // Disable irrelevant buttons
        // button_saveText: always visible
        // button_showImage: visible iff images available
        int rkeyImages = getIntent().getIntExtra(EXTRA_IMAGES_RKEY, -1);
        Bitmap images[] = (Bitmap[])IntentExtraStorage.tryRetrieve(rkeyImages);
        if (images == null)
            images = new Bitmap[0];

        findViewById(R.id.button_showImage).setVisibility(images.length == 0 ? View.GONE : View.VISIBLE);

        // button_retakeImage: visible if LOCAL and from camera
        boolean showRetakeImage = getIntent().getBooleanExtra(EXTRA_SHOW_RETAKE_IMAGE, false);
        findViewById(R.id.button_retakeImage).setVisibility(showRetakeImage ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        // Clean up "intent" resources
        int rkeyImages = getIntent().getIntExtra(EXTRA_IMAGES_RKEY, -1);
        IntentExtraStorage.evictObject(rkeyImages);
        super.onDestroy();
    }
    private static String formatDate (Date date)
    {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        return format.format(date) + " (local time)";
    }

    private static String formatPageText (String pages[])
    {
        StringBuilder combinedTextBuilder = new StringBuilder();
        for (int ndx = 0; ndx < pages.length; ++ndx)
        {
            combinedTextBuilder.append("\n");
            combinedTextBuilder.append("------");
            combinedTextBuilder.append(" Page ");
            combinedTextBuilder.append(ndx);
            combinedTextBuilder.append(" ");
            combinedTextBuilder.append("------\n\n");
            combinedTextBuilder.append(pages[ndx]);
        }
        return combinedTextBuilder.toString();
    }

    public void onClickSaveText (View view)
    {
        String fullText = formatPageText(getIntent().getStringArrayExtra(EXTRA_OCR_TEXT));
        File dstFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "OcrText.txt");
        try
        {
            dstFile.getParentFile().mkdirs();

            FileOutputStream os = new FileOutputStream(dstFile);
            PrintWriter pw = new PrintWriter(os);
            pw.write(fullText);
            pw.close();
            os.close();

            Toast.makeText(this, "Wrote to " + dstFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException ex) {
            Toast.makeText(this, "Faield to write, got " + ex.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickRetakeImage (View view)
    {
        // TODO: retake image
        Intent intent = new Intent(ShowOCRTextScreen.this, Ocr_Operations.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("EXTRA_IS_RETAKE", true);
        startActivity(intent);
    }

    public void onClickShowImages (View view)
    {
        int rkeyImages = getIntent().getIntExtra(EXTRA_IMAGES_RKEY, -1);
        Intent intent = new Intent(this, ShowImagesActivity.class);
        intent.putExtra(ShowImagesActivity.EXTRA_IMAGES_RKEY, rkeyImages);
        startActivity(intent);
    }
}
