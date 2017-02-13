package mcc_2016_g05_p2.niksula.hut.fi.android;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import mcc_2016_g05_p2.niksula.hut.fi.ocrengine.TesseractEngine;
import mcc_2016_g05_p2.niksula.hut.fi.rpc.FakeRemoteRPC;
import mcc_2016_g05_p2.niksula.hut.fi.rpc.IRemoteRPC;
import mcc_2016_g05_p2.niksula.hut.fi.rpc.RemoteRPC;

public class LoginScreen extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "MCC-2016-SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private ProgressDialog mRPCPendingDialog;
    public static GoogleApiClient mGoogleApiClient;
    static IRemoteRPC mRPC;
    ConnectivityListener mConnectivityListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);

        // Inject tesseract config
        TesseractEngine.injectConfig(getAssets(), getApplicationInfo().dataDir);

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
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

        // TOGGLE UI
        View buttonView = findViewById(R.id.sign_in_button);
        View errorView = findViewById(R.id.no_connection_text);
        if (buttonView != null)
            buttonView.setVisibility(isConnected ? View.VISIBLE : View.GONE);
        if (errorView != null)
            errorView.setVisibility(isConnected ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected class WaitLoginTask extends AsyncTask<Void, Void, RemoteRPC.OpenResult>
    {
        private Future<RemoteRPC.OpenResult> m_future;
        public WaitLoginTask (Future<RemoteRPC.OpenResult> future) {
            m_future = future;
        }
        @Override
        protected RemoteRPC.OpenResult doInBackground(Void... params) {
            try
            {
                return m_future.get();
            }
            catch (InterruptedException|ExecutionException|CancellationException ex)
            {
                RemoteRPC.OpenResult result = new RemoteRPC.OpenResult();
                result.errorString = ex.toString();
                return result;
            }
        }
        @Override
        protected void onPostExecute(RemoteRPC.OpenResult result) {
            mRPCPendingDialog.dismiss();
            mRPCPendingDialog = null;

            if (result.rpc != null) {
                // success
                LoginScreen.mRPC = result.rpc;
                Intent intent = new Intent(LoginScreen.this, Ocr_Operations.class);
                startActivity(intent);
            } else {
                // error
                Toast.makeText(getApplicationContext(), result.errorString, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            /// IF YOU WANT TO JUST TEST, UNCOMMENT HERE
            //LoginScreen.mRPC = new FakeRemoteRPC();
            //Intent intent = new Intent(this, Ocr_Operations.class); // or PROTO_ShowHistoryScreen.class
            //startActivity(intent);
            //if (intent != null) // always true
            //    return;
            /// END OF TEST BLOCK

            if (result.isSuccess()) {
                // Start login to backend
                Future<RemoteRPC.OpenResult> openFuture = RemoteRPC.beginOpen();

                mRPCPendingDialog = ProgressDialog.show(this, "Logging in", "Please wait, logging you in...", true);
                new WaitLoginTask(openFuture).execute();
            } else {
                String msg = getResources().getString(R.string.sign_in_err);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onClick(View v) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
}
