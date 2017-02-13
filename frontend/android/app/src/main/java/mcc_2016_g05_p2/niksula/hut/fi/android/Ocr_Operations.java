package mcc_2016_g05_p2.niksula.hut.fi.android;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


import mcc_2016_g05_p2.niksula.hut.fi.ocrdriver.BenchmarkOCRDriver;
import mcc_2016_g05_p2.niksula.hut.fi.ocrdriver.IOCRDriver;
import mcc_2016_g05_p2.niksula.hut.fi.ocrdriver.LocalOCRDriver;
import mcc_2016_g05_p2.niksula.hut.fi.ocrdriver.RemoteOCRDriver;
import mcc_2016_g05_p2.niksula.hut.fi.rpc.IRemoteRPC;


public class Ocr_Operations extends AppCompatActivity {

    RadioGroup OperationControl;
    private static int OPERATION_SELECT = 2;
    private int PICK_IMAGE_REQUEST = 1;
    private int CAMERA_PIC_REQUEST = 2;
    private Uri photoURI;
    private String pictureImagePath = "";
    private ArrayList<Bitmap> ImagesList = new ArrayList<>();
    ProgressDialog mDriverDialog;
    private boolean wasCamera = false;
    private RelativeLayout historyLayout, ocrLayout;
    ConnectivityListener mConnectivityListener;
    boolean mIsConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_operations);
        historyLayout = (RelativeLayout) findViewById(R.id.layout_history_screen);
        ocrLayout = (RelativeLayout) findViewById(R.id.layout_OcrScreen);
        OperationControl = (RadioGroup) findViewById(R.id.operationGroup);
        OperationControl.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId){
                    case R.id.localOp:
                        // do operations specific to this selection
                        OPERATION_SELECT = 1;
//                        Toast.makeText(Ocr_Operations.this, "LOCAL SELECTED", Toast.LENGTH_LONG);
                        break;
                    case R.id.remoteOp:
                        // do operations specific to this selection
                        OPERATION_SELECT = 2;
                        break;
                    case R.id.benchmarkOp:
                        // do operations specific to this selection
                        OPERATION_SELECT = 3;
                        break;
                }
            }
        });
        InitActivity();

        // Context-sensitive login button
        mConnectivityListener = new ConnectivityListener();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityListener, filter);
        checkConnection();
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(mConnectivityListener);
        super.onDestroy();
    }

    class ConnectivityListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkConnection();
        }
    };
    private void checkConnection ()
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        mIsConnected = activeNetwork != null && activeNetwork.isConnected();

        // TOGGLE UI
        if (mIsConnected)
        {
            findViewById(R.id.remoteOp).setClickable(true);
            findViewById(R.id.benchmarkOp).setClickable(true);
        }
        else
        {
            OperationControl.check(R.id.localOp);
            findViewById(R.id.remoteOp).setClickable(false);
            findViewById(R.id.benchmarkOp).setClickable(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateItemList();
    }

    private void InitActivity(){
        switch (OPERATION_SELECT){
            case  1:
                OperationControl.check(R.id.localOp);
                break;
            case 2:
                OperationControl.check(R.id.remoteOp);
                break;
            case 3:
                OperationControl.check(R.id.benchmarkOp);
                break;
        }
        boolean messageFromOCRtextSC = getIntent().getBooleanExtra("EXTRA_IS_RETAKE", false);
        if(messageFromOCRtextSC){
            ToggleAction();
            PerformCameraActions();
        } else {
            updateItemList();
        }
        ImagesList.clear();
    }

    public void SelectImagefromGallery(View view){
        // TODO: MULTIPLE IMAGE SELECTION FROM GALLERY
        ImagesList.clear();
        Intent intent = new Intent();
        intent.setType("image/*");
        // It shows multiple image selection from recent images. But not from the
        // gallery and I couldn't figure out how to get the selected images
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    public void CameraBtnPressed(View view){
        ImagesList.clear();
        int version = android.os.Build.VERSION.SDK_INT;
        if ((version > android.os.Build.VERSION_CODES.LOLLIPOP) && ContextCompat.checkSelfPermission(Ocr_Operations.this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(Ocr_Operations.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Ocr_Operations.this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        CAMERA_REQUEST_PERMISSION);
        }
        else {

            PerformCameraActions();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {

            ArrayList<Uri> uris = new ArrayList<>();
            ClipData clipData = data.getClipData();

            if (data.getData() != null)
                uris.add(data.getData());
            else if (clipData != null)
            {
                for (int ndx = 0; ndx < clipData.getItemCount(); ++ndx)
                    uris.add(clipData.getItemAt(ndx).getUri());
            }

            try {
                for (Uri uri : uris) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
//                ImageView imageView = (ImageView) findViewById(R.id.imageView);
//                imageView.setImageBitmap(bitmap);
                    ImagesList.add(bitmap);
                }
                wasCamera = false;
                if (!ImagesList.isEmpty())
                    PerformOcr();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if (requestCode == CAMERA_PIC_REQUEST && resultCode == RESULT_OK){
            File imgFile = new  File(pictureImagePath);
            if(imgFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//                ImageView myImage = (ImageView) findViewById(R.id.imageView);
//                myImage.setImageBitmap(myBitmap);
                ImagesList.add(myBitmap);
                wasCamera = true;
                PerformOcr();
            }
        }
    }

    private final int HISTORY = 2354;
    private final int OCR = 5402;
    private int CURRENT_LAYOUT = HISTORY;
    public void ToggleLayout(View view){
        ToggleAction();
    }
    private void ToggleAction(){
        if(CURRENT_LAYOUT == HISTORY) {
            historyLayout.setVisibility(View.INVISIBLE);
            ocrLayout.setVisibility(View.VISIBLE);
            CURRENT_LAYOUT = OCR;
        } else {
            historyLayout.setVisibility(View.VISIBLE);
            ocrLayout.setVisibility(View.INVISIBLE);
            CURRENT_LAYOUT = HISTORY;
        }
    }
    @Override
    public void onBackPressed(){
        if(CURRENT_LAYOUT == OCR) {
            updateItemList();
            ToggleAction();
        }
        else {
            OPERATION_SELECT = 2;
            super.onBackPressed();
        }
    }
    private void PerformCameraActions(){
        Log.d("CAMERA", "???CAMERA???");
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Create the File where the photo should go
        File photoFile = null;
        try {
            createImageFile();
        } catch (Exception ex) {
            // Error occurred while creating the File

        }
        // Continue only if the File was successfully created
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    private void createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName;
        File image = new File(pictureImagePath);
        photoURI = Uri.fromFile(image);
    }

    private File createImageFile(String part, String ext) throws Exception
    {
        File tempDir= Environment.getExternalStorageDirectory();
        tempDir=new File(tempDir.getAbsolutePath()+"/.temp/");
        if(!tempDir.exists())
        {
            tempDir.mkdirs();
        }
        return File.createTempFile(part, ext, tempDir);
    }



    private final int CAMERA_REQUEST_PERMISSION = 898;
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    PerformCameraActions();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(Ocr_Operations.this, "Permission Not Granted", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    protected class WaitDriverTask extends AsyncTask<Future<IOCRDriver.Result>, Void, IOCRDriver.Result>
    {
        @Override
        protected IOCRDriver.Result doInBackground(Future<IOCRDriver.Result>... params) {
            try {
                return params[0].get();
            } catch (InterruptedException|ExecutionException |CancellationException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(IOCRDriver.Result result) {
            mDriverDialog.dismiss();
            if (result == null)
            {
                Toast.makeText(Ocr_Operations.this, "failed", Toast.LENGTH_LONG).show();
            }
            else
            {
                Intent intent = new Intent(Ocr_Operations.this, result.resultActivityClass);
                intent.putExtras(result.extras);
                startActivity(intent);
            }
        }
    }

    public  void PerformOcr(){
        IOCRDriver driver = null;
        switch (OPERATION_SELECT){
            case 1:
                driver = new LocalOCRDriver();
                break;
            case 2:
                driver = new RemoteOCRDriver(LoginScreen.mRPC);
                break;
            case 3:
                driver = new BenchmarkOCRDriver(LoginScreen.mRPC);
                break;
        }

        final Future<IOCRDriver.Result> f = driver.beginProcess(ImagesList.toArray(new Bitmap[0]), wasCamera);
        mDriverDialog = ProgressDialog.show(this, "Processing", "Please Wait", true);
        new Ocr_Operations.WaitDriverTask().execute(f);
    }

    //SHOW HISTORY SCREEN CODE
    private ProgressDialog m_imageFetchProcessingDialog = null;

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
            linearLayoutMain.addView(recordView, 0);
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
            {
                if (mIsConnected)
                    Toast.makeText(Ocr_Operations.this, "Images not available, possibly expired", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(Ocr_Operations.this, "Offline, images not available", Toast.LENGTH_SHORT).show();
            }

            Intent intent = new Intent(Ocr_Operations.this, ShowOCRTextScreen.class);
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
        if (mIsConnected) {
            for (int page = 0; page < record.pages.length; ++page)
                pagePics.add(LoginScreen.mRPC.beginResolveImage(record.pages[page].imageId));
        }

        m_imageFetchProcessingDialog = ProgressDialog.show(this, "Fetching", "Fetching data, please wait...", true);

        new Ocr_Operations.ShowHistoryItemTask(record).execute(pagePics);
    }
}
