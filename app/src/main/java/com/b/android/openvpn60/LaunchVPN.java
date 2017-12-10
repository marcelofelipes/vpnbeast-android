package com.b.android.openvpn60;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.b.android.openvpn60.activity.ActivityStatus;
import com.b.android.openvpn60.activity.MainActivity;
import com.b.android.openvpn60.core.OpenVPNStatusService;
import com.b.android.openvpn60.core.PasswordCache;
import com.b.android.openvpn60.core.Preferences;
import com.b.android.openvpn60.core.ProfileManager;
import com.b.android.openvpn60.core.VPNLaunchHelper;
import com.b.android.openvpn60.core.VpnStatus;
import com.b.android.openvpn60.enums.Constants;

import java.io.IOException;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IServiceStatus;

/**
 * Created by b on 5/15/17.
 */

public class LaunchVPN extends Activity {
    public static final String EXTRA_KEY = "shortcutProfileUUID";
    public static final String EXTRA_NAME = "shortcutProfileName";
    public static final String EXTRA_HIDELOG = "showNoLogWindow";
    public static final String CLEARLOG = "clearlogconnect";
    public static final String RESULT_PROFILE = Constants.RESULT_PROFILE.toString();

    private static final int START_VPN_PROFILE = 70;
    private static final String TAG = LaunchVPN.class.toString();

    private VpnProfile mSelectedProfile;
    private String mTransientAuthPW;
    private String mTransientCertOrPCKS12PW;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.launchvpn);
        startVpnFromIntent();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            IServiceStatus service = IServiceStatus.Stub.asInterface(binder);
            try {
                if (mTransientAuthPW != null)

                    service.setCachedPassword(mSelectedProfile.getUUIDString(), PasswordCache.AUTHPASSWORD, mTransientAuthPW);
                if (mTransientCertOrPCKS12PW != null)
                    service.setCachedPassword(mSelectedProfile.getUUIDString(), PasswordCache.PCKS12ORCERTPASSWORD, mTransientCertOrPCKS12PW);

                onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);

            } catch (RemoteException e) {
                e.printStackTrace();
            }

            unbindService(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    protected void startVpnFromIntent() {
        // Resolve the intent

        final Intent intent = getIntent();
        final String action = intent.getAction();

        // If the intent is a request to create a shortcut, we'll do that and exit


        if (Intent.ACTION_MAIN.equals(action)) {
            // Check if we need to clear the log
            if (Preferences.getDefaultSharedPreferences(this).getBoolean(CLEARLOG, true))
                VpnStatus.clearLog();

            // we got called to be the starting point, most likely a shortcut
            String shortcutUUID = intent.getStringExtra(EXTRA_KEY);
            String shortcutName = intent.getStringExtra(EXTRA_NAME);

            VpnProfile profileToConnect = MainActivity.profile;
            //VpnProfile profileToConnect = ProfileManager.get(this, shortcutUUID);
            if (shortcutName != null && profileToConnect == null)
                profileToConnect = ProfileManager.getInstance(this).getProfileByName(shortcutName);

            if (profileToConnect == null) {
                //Handle that later
                Toast.makeText(getApplicationContext(), "null", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                mSelectedProfile = profileToConnect;
                launchVPN();
                Toast.makeText(getApplicationContext(), "Launching", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void askForPW(final int type) {

        final EditText entry = new EditText(this);
        final View userpwlayout = getLayoutInflater().inflate(R.layout.userpw, null, false);

        entry.setSingleLine();
        entry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        entry.setTransformationMethod(new PasswordTransformationMethod());

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.pw_request_dialog_title, getString(type)));
        dialog.setMessage(getString(R.string.pw_request_dialog_prompt, mSelectedProfile.name));

        if (type == R.string.password) {
            ((EditText) userpwlayout.findViewById(R.id.username)).setText(mSelectedProfile.userName);
            ((EditText) userpwlayout.findViewById(R.id.password)).setText(mSelectedProfile.password);
            ((CheckBox) userpwlayout.findViewById(R.id.save_password)).setChecked(!TextUtils.isEmpty(mSelectedProfile.password));
            ((CheckBox) userpwlayout.findViewById(R.id.show_password)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked)
                        ((EditText) userpwlayout.findViewById(R.id.password)).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    else
                        ((EditText) userpwlayout.findViewById(R.id.password)).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            });

            dialog.setView(userpwlayout);
        } else {
            dialog.setView(entry);
        }

        AlertDialog.Builder builder = dialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (type == R.string.password) {
                            mSelectedProfile.userName = ((EditText) userpwlayout.findViewById(R.id.username)).getText().toString();

                            String pw = ((EditText) userpwlayout.findViewById(R.id.password)).getText().toString();
                            if (((CheckBox) userpwlayout.findViewById(R.id.save_password)).isChecked()) {
                                mSelectedProfile.password = pw;
                            } else {
                                mSelectedProfile.password = null;
                                mTransientAuthPW = pw;
                            }
                        } else {
                            mTransientCertOrPCKS12PW = entry.getText().toString();
                        }
                        Intent intent = new Intent(LaunchVPN.this, OpenVPNStatusService.class);
                        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    }

                });
        dialog.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        VpnStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "", R.string.state_user_vpn_password_cancelled,
                                ConnectionStatus.LEVEL_NOTCONNECTED);
                        finish();
                    }
                });

        dialog.create().show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == Activity.RESULT_OK) {
                int needpw = mSelectedProfile.needUserPWInput(mTransientCertOrPCKS12PW, mTransientAuthPW);
                if (needpw != 0) {
                    VpnStatus.updateStateString("USER_VPN_PASSWORD", "", R.string.state_user_vpn_password,
                            ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
                    askForPW(needpw);
                } else {
                    SharedPreferences prefs = Preferences.getDefaultSharedPreferences(this);
                    ProfileManager.updateLRU(this, mSelectedProfile);
                    VPNLaunchHelper.startOpenVpn(MainActivity.profile, getBaseContext());
                    Log.i(TAG, "Really lauching");
                    showAfterMain();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User does not want us to start, so we just vanish
                VpnStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "", R.string.state_user_vpn_permission_cancelled,
                        ConnectionStatus.LEVEL_NOTCONNECTED);

                finish();
            }
        }
    }

    void showAfterMain() {

        Intent startLW = new Intent(getBaseContext(), ActivityStatus.class);
        startLW.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startLW.putExtra(RESULT_PROFILE, mSelectedProfile);
        startActivity(startLW);
        this.finish();
    }

    void showConfigErrorDialog(int vpnok) {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle(R.string.config_error_found);
        d.setMessage(vpnok);
        d.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();

            }
        });
        d.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            setOnDismissListener(d);
        d.show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void setOnDismissListener(AlertDialog.Builder d) {
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
    }

    void launchVPN() {
        /*int vpnok = mSelectedProfile.checkProfile(this);
        if (vpnok != R.string.no_error_found) {
            showConfigErrorDialog(vpnok);
            return;
        }*/

        Intent intent = VpnService.prepare(this);

        execeuteSUcmd("chown system /dev/tun");

        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            // Start the query
            try {
                startActivityForResult(intent, START_VPN_PROFILE);
            } catch (ActivityNotFoundException ane) {
                // Shame on you Sony! At least one user reported that
                // an official Sony Xperia Arc S image triggers this exception
                VpnStatus.logError(R.string.no_vpn_support_image);
            }
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }

    }

    private void execeuteSUcmd(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", command);
            Process p = pb.start();
            int ret = p.waitFor();
            //if (ret == 0)
            //mCmfixed = true;
        } catch (InterruptedException | IOException e) {
            VpnStatus.logException("SU command", e);
        }
    }
}