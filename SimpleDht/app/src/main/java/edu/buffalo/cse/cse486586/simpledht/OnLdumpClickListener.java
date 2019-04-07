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
    private final Uri uri;


    @Override
    public void onClick(View v) {
        new Task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public OnLdumpClickListener(TextView _tv, ContentResolver _cr) {
        textView = _tv;
        contentResolver = _cr;
        uri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
    }

    private Uri buildUri(String scheme, String authority)
    {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class Task extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Cursor cursor = contentResolver.query(uri, null, "@", null, null);
            //Code referenced from http://stackoverflow.com/questions/2810615/how-to-retrieve-data-from-cursor-class
            if(cursor.moveToFirst()) {
                while(!cursor.isAfterLast()) {
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