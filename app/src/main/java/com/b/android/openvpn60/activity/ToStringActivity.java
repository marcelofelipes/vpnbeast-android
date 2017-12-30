package com.b.android.openvpn60.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.core.ProfileManager;
import com.b.android.openvpn60.model.VpnProfile;
import com.b.android.openvpn60.model.VpnProfileTest;

public class ToStringActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_string);
        TextView txtTostring = (TextView) this.findViewById(R.id.txtTostring);
        //txtTostring.setText(this.getIntent().getSerializableExtra(AppConstants.RESULT_PROFILE.toString()).toString());

        VpnProfileTest test = new VpnProfileTest();
        Intent statusIntent = new Intent(ToStringActivity.this, StatusActivity.class);
        statusIntent.putExtra(AppConstants.RESULT_PROFILE.toString(), test);
        startOrStopVPN(test);
    }

    private void startOrStopVPN(VpnProfileTest profile) {
        startVPN(profile);
    }

    private void startVPN(VpnProfileTest profile) {
        getPM().saveProfile(this, profile);
        Intent intent = new Intent(this, LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
        this.finish();
    }

    private ProfileManager getPM() {
        return ProfileManager.getInstance(this);
    }
}
