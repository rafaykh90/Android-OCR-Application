package mcc_2016_g05_p2.niksula.hut.fi.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import mcc_2016_g05_p2.niksula.hut.fi.rpc.IRemoteRPC;

public class PROTO_ShowHistoryScreen extends AppCompatActivity {
    private ProgressDialog m_imageFetchProcessingDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proto__show_history_screen);

        updateItemList();
    }

    private void updateItemList()
    {
        List<IRemoteRPC.Record> history = LoginScreen.mRPC.getHistory();
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);

        LinearLayout linearLayoutMain = (LinearLayout) findViewById(R.id.linearLayoutHistory);
        linearLayoutMain.removeAllViews();

        for (IRemoteRPC.Record record : history)
        {
            View recordView = layoutInflater.inflate(R.layout.history_item, linearLayoutMain, false);

            StringBuilder previewText = new StringBuilder();
            previewText.append(record.pages.length);
            previewText.append(" page(s)\n\n");

            int maxLen = 80;
            if (record.pages[0].text.length() > maxLen)
            {
                previewText.append(record.pages[0].text.substring(0,maxLen));
                previewText.append("...");
            }
            else
                previewText.append(record.pages[0].text);

            ((TextView)recordView.findViewById(R.id.histItem_text)).setText(previewText);
            ((ImageView)recordView.findViewById(R.id.histItem_img)).setImageBitmap(record.pages[0].thumb);
            recordView.findViewById(R.id.histItem_btn).setTag(record);
            linearLayoutMain.addView(recordView);
        }
    }

    protected class ShowHistoryItemTask extends AsyncTask<ArrayList<Future<Bitmap>>, Void, ArrayList<Bitmap>>
    {
        private final IRemoteRPC.Record m_record;
        public ShowHistoryItemTask (IRemoteRPC.Record record)
        {
            m_record = record;
        }

        @Override
        protected ArrayList<Bitmap> doInBackground(ArrayList<Future<Bitmap>>... params) {
            try {
                ArrayList<Bitmap> resolved = new ArrayList<Bitmap>();
                for (Future<Bitmap> param : params[0])
                    resolved.add(param.get());
                return resolved;
            } catch (InterruptedException|ExecutionException|CancellationException e) {
                return new ArrayList<Bitmap>();
            }
        }
        @Override
        protected void onPostExecute (ArrayList<Bitmap> resolvedPages)
        {
            m_imageFetchProcessingDialog.dismiss();
            m_imageFetchProcessingDialog = null;

            String pageTexts[] = new String[m_record.pages.length];
            for (int ndx = 0; ndx < m_record.pages.length; ++ndx)
                pageTexts[ndx] = m_record.pages[ndx].text;
            Bitmap[] resolvedPagesArray = (Bitmap[])resolvedPages.toArray(new Bitmap[0]);

            if (resolvedPagesArray.length == 0)
                Toast.makeText(PROTO_ShowHistoryScreen.this, "Images not available, possibly expired", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(PROTO_ShowHistoryScreen.this, ShowOCRTextScreen.class);
            intent.putExtra(ShowOCRTextScreen.EXTRA_OCR_TEXT, pageTexts);
            intent.putExtra(ShowOCRTextScreen.EXTRA_OCR_DATE, m_record.date);
            intent.putExtra(ShowOCRTextScreen.EXTRA_IMAGES_RKEY, IntentExtraStorage.storeObject(resolvedPagesArray));
            intent.putExtra(ShowOCRTextScreen.EXTRA_SHOW_RETAKE_IMAGE, false);
            startActivity(intent);
        }
    }
    public void onHistoryItemClicked (View view)
    {
        IRemoteRPC.Record record = (IRemoteRPC.Record)view.getTag();

        final ArrayList<Future<Bitmap>> pagePics = new ArrayList<>();
        for (int page = 0; page < record.pages.length; ++page)
            pagePics.add(LoginScreen.mRPC.beginResolveImage(record.pages[page].imageId));

        m_imageFetchProcessingDialog = ProgressDialog.show(this, "Fetching", "Fetching data, please wait...", true);

        new ShowHistoryItemTask(record).execute(pagePics);
    }
}
