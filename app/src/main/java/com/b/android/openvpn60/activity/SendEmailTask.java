package com.b.android.openvpn60.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.core.GmailSender;
import com.b.android.openvpn60.core.Mail;
import com.b.android.openvpn60.helper.LogHelper;

/**
 * Created by b on 9/19/17.
 */

public class SendEmailTask extends AsyncTask {
    private ProgressDialog statusDialog;
    private Activity sendMailActivity;
    public static boolean processFlag = false;
    private LogHelper logHelper;

    public SendEmailTask(Activity activity) {
        sendMailActivity = activity;
    }


    protected void onPreExecute() {
        logHelper = LogHelper.getLogHelper(sendMailActivity);
        logHelper.logInfo("Preparing to send email...");
        statusDialog = new ProgressDialog(sendMailActivity);
        statusDialog.setMessage("Getting ready...");
        statusDialog.setIndeterminate(false);
        statusDialog.setCancelable(false);
        statusDialog.show();
    }

    @Override
    protected Object[] doInBackground(Object... args) {
        try {
            GmailSender sender = new GmailSender("bilalccaliskan@gmail.com","piranha93");
            try {
                sender.sendMail("Reset Password", "This email will contain reset password link",
                        "bilalccaliskan@gmail.com", "bilalccaliskan@gmail.com", "");
            } catch (Exception e) {
                logHelper.logException(e);
            }
        } catch (Exception e) {
            logHelper.logException(e);
        }
        return null;
    }

    @Override
    public void onProgressUpdate(Object... values) {
        statusDialog.setMessage(values[0].toString());
    }

    @Override
    public void onPostExecute(Object result) {
        statusDialog.dismiss();
        Toast.makeText(sendMailActivity.getApplicationContext(), sendMailActivity.getString(R.string.text_email_sent),
                Toast.LENGTH_SHORT).show();
    }
}