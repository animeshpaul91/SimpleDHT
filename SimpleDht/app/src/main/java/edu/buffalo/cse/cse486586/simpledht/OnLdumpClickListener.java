package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class OnLdumpClickListener implements OnClickListener {


    private final TextView textView;
    private final ContentResolver contentResolver;
    private final Uri providerUri;
    private static final String my_dht = "@";


    @Override
    public void onClick(View v) {
        new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public OnLdumpClickListener(TextView tv, ContentResolver cr) {
        textView = tv;
        contentResolver = cr;
        providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider"); //URI
    }


    private class Task extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Cursor cursor = contentResolver.query(providerUri, null, my_dht, null, null);
            //Code referenced from http://stackoverflow.com/questions/2810615/how-to-retrieve-data-from-cursor-class
            if(cursor.moveToFirst()) {
                while(cursor.moveToNext()) {
                    String key = cursor.getString(cursor.getColumnIndex("key"));
                    String value = cursor.getString(cursor.getColumnIndex("value"));
                    publishProgress(key + ":" + value + "\n");
                    cursor.moveToNext();
                }
            }
            cursor.close();
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            textView.append(strings[0]);
            return;
        }
    }
}