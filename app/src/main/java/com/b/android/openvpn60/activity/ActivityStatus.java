package com.b.android.openvpn60.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.b.android.openvpn60.core.OpenVPNManagement;
import com.b.android.openvpn60.core.OpenVPNManagementThread;
import com.b.android.openvpn60.core.OpenVPNService;
import com.b.android.openvpn60.core.OpenVPNThread;
import com.b.android.openvpn60.core.ProfileManager;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.VpnProfile;
import com.b.android.openvpn60.core.VpnStatus;
import com.b.android.openvpn60.enums.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;

public class ActivityStatus extends AppCompatActivity implements VpnStatus.StateListener, VpnStatus.ByteCountListener {
    public static final String RESULT_PROFILE = Constants.RESULT_PROFILE.toString();
    public static final String DISCONNECT_VPN = "DISCONNECT_VPN";
    public static final String CLOSE_ACTIVITY = "CLOSE_ACTIVITY";
    public static final String RESULT_DESTROYED = "RESULT_DESTROYED";
    public static final String TAG = ActivityStatus.class.toString();
    private long hours = 00;
    private long minutes = 00;
    private long seconds = 00;
    private long mConnectTime;
    private boolean isBytesDisplayed = false;
    private static ProgressDialog progressDialog;
    private static VpnProfile mProfile;
    private EditText edtUser;
    private EditText edtLocation;
    private EditText edtIp;
    private EditText edtPort;
    private EditText edtProfile;
    private EditText edtDuration;
    private EditText edtBytesIn;
    private EditText edtBytesOut;
    private EditText edtStatus;
    private Button btnDisconnect;
    private static AsyncTask<Void, Void, Integer> checker;
    private static Intent intent;
    private Runnable runnable;
    private static SharedPreferences sharedPrefs;
    private static Context context;
    private IOpenVPNServiceInternal serviceInternal;
    private ServiceConnection mConnection;
    private String lastStatus = "";
    private boolean isDestroyed = false;
    private OpenVPNService instance;


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        //bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        //updateViews();
        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);
        Intent intent = new Intent(ActivityStatus.this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        init();
        updateViews();
        runnable.run();
    }


    private void init() {
        progressDialog = new ProgressDialog(ActivityStatus.this, AlertDialog.THEME_HOLO_DARK);
        progressDialog.setProgressStyle(R.style.ProgressBar);
        btnDisconnect = (Button) this.findViewById(R.id.btnDisconnect);
        mProfile = (VpnProfile) getIntent().getSerializableExtra(RESULT_PROFILE);
        edtUser = (EditText) this.findViewById(R.id.edtUser);
        edtProfile = (EditText) this.findViewById(R.id.edtProfile);
        edtIp = (EditText) this.findViewById(R.id.edtIp);
        edtPort = (EditText) this.findViewById(R.id.edtPort);
        sharedPrefs = this.getSharedPreferences(Constants.SHARED_PREFS.toString(), MODE_PRIVATE);
        edtStatus = (EditText) this.findViewById(R.id.edtStatus);
        edtDuration = (EditText) this.findViewById(R.id.edtDuration);
        context = this.getApplicationContext();
        edtBytesIn = (EditText) this.findViewById(R.id.edtBytesIn);
        edtBytesOut = (EditText) this.findViewById(R.id.edtBytesOut);
        edtUser.setText(sharedPrefs.getString(Constants.USER_NAME.toString(), null));
        intent = new Intent(this, MainActivity.class);

        runnable = new Runnable() {
            @Override
            public void run() {
                mConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName className,
                                                   IBinder service) {
                        serviceInternal = IOpenVPNServiceInternal.Stub.asInterface(service);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName arg0) {
                        serviceInternal = null;
                    }

                };
            }
        };
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });
    }


    private void updateViews() {

        checker = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected void onPreExecute() {
                if (VpnStatus.mLastLevel != ConnectionStatus.LEVEL_CONNECTED) {
                    progressDialog.setTitle(R.string.state_connecting);
                    progressDialog.setMessage(getString(R.string.state_msg_connecting));
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            }

            @Override
            protected Integer doInBackground(Void... params) {
                while (VpnStatus.mLastLevel != ConnectionStatus.LEVEL_CONNECTED) {
                    lastStatus = ActivityStatus.this.getIntent().getStringExtra("status");
                }
                progressDialog.dismiss();

                return 0;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                mConnectTime = System.currentTimeMillis();

                edtProfile.setText(mProfile.name);
                edtIp.setText(mProfile.connections[0].serverName);
                edtPort.setText(mProfile.connections[0].serverPort);
                edtStatus.setText(getString(R.string.state_connected));
                btnDisconnect.setText(getString(R.string.disconnect));
                btnDisconnect.setBackgroundColor(Color.parseColor("#df4a4a"));
                isBytesDisplayed = true;
                VpnStatus.addByteCountListener(ActivityStatus.this);
                VpnStatus.addStateListener(ActivityStatus.this);
                updateDuration();
                Intent intent = new Intent(ActivityStatus.this, OpenVPNService.class);
                intent.setAction(OpenVPNService.START_SERVICE);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            }
        }.execute();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    private String humanReadableByteCount(long bytes, boolean mbit) {
        if (mbit)
            bytes = bytes * 8;
        int unit = mbit ? 1000 : 1024;
        if (bytes < unit)
            return bytes + (mbit ? " bit" : " B");

        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (mbit ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (mbit ? "" : "");
        if (mbit)
            return String.format(Locale.getDefault(), "%.1f %sbit", bytes / Math.pow(unit, exp), pre);
        else
            return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, R.string.state_already_connected_msg, Toast.LENGTH_SHORT).show();
        //super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        VpnStatus.removeStateListener(this);
        VpnStatus.removeByteCountListener(this);

    }


    @Override
    protected void onDestroy() {
        //disconnectOnDestroy();
        unbindService(mConnection);
        super.onDestroy();
    }

    private void disconnectOnDestroy() {
        ProfileManager.setConntectedVpnProfileDisconnected(context);
        if (serviceInternal != null) {
            try {
                serviceInternal.stopVPN(false);
                VpnStatus.mLastLevel = ConnectionStatus.LEVEL_NOTCONNECTED;
            } catch (RemoteException e) {
                VpnStatus.logException(e);
            }
        }
        unbindService(mConnection);
    }


    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        final String mIn = humanReadableByteCount(in, false);
        final String mIns = humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true);
        final String mOut = humanReadableByteCount(out, false);
        final String mOuts = humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true);

        if (VpnStatus.mLastLevel == ConnectionStatus.LEVEL_CONNECTED) {

            ActivityStatus.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateByteTexts(mIn, mIns, mOut, mOuts);

                }
            });
        }
    }


    private void updateDuration() {
        if (VpnStatus.mLastLevel == ConnectionStatus.LEVEL_CONNECTED) {

            Timer timer = new Timer();
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    ActivityStatus.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seconds = (System.currentTimeMillis() - mConnectTime) / 1000;
                            Calendar cal = Calendar.getInstance();
                            cal.set(Calendar.HOUR_OF_DAY, (int) hours);
                            cal.set(Calendar.MINUTE, (int) minutes);
                            cal.set(Calendar.SECOND, (int) seconds);
                            Date date = cal.getTime();
                            SimpleDateFormat mSimple = new SimpleDateFormat("HH:mm:ss");
                            edtDuration.setText(mSimple.format(date));
                        }
                    });
                }
            }, 0, 1000);
        }
    }


    private void updateByteTexts(String in, String ins, String out, String outs) {
        edtBytesIn.setText(ins + " / " + in);
        edtBytesOut.setText(outs + " / " + out);
    }


    private void disconnect() {

        disconnectVpn();
    }


    private void disconnectVpn() {
        checker = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected void onPreExecute() {
                context = getApplicationContext();
                progressDialog.setTitle(R.string.state_disconnecting);
                progressDialog.setMessage(getString(R.string.state_msg_disconnecting));
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected Integer doInBackground(Void... params) {
                ProfileManager.setConntectedVpnProfileDisconnected(context);
                if (serviceInternal != null) {
                    try {
                        serviceInternal.stopVPN(false);
                        VpnStatus.mLastLevel = ConnectionStatus.LEVEL_NOTCONNECTED;
                    } catch (RemoteException e) {
                        VpnStatus.logException(e);
                    }
                }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(ActivityStatus.this, AlertDialog.THEME_HOLO_DARK);
                mBuilder.setTitle(getString(R.string.state_disconnected));
                mBuilder.setMessage(R.string.state_msg_disconnected);
                progressDialog.dismiss();
                mBuilder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        intent.putExtra(DISCONNECT_VPN, true);
                        VpnStatus.removeStateListener(ActivityStatus.this);
                        VpnStatus.removeByteCountListener(ActivityStatus.this);
                        ActivityStatus.this.finish();
                        if (isDestroyed) {
                            Intent intentMain = new Intent(ActivityStatus.this, MainActivity.class);
                            intentMain.putExtra(RESULT_DESTROYED, isDestroyed);
                            ActivityStatus.this.startActivity(intentMain);
                        }

                    }
                });
                mBuilder.show();
            }
        }.execute();
    }


    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level) {

    }


    @Override
    public void setConnectedVPN(String uuid) {

    }
}