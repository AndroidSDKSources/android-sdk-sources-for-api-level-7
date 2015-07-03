package com.android.providers.contacts;

import android.app.Service;
import android.os.IBinder;
import android.content.Intent;
import android.content.ContentProviderClient;
import android.content.ContentProvider;
import android.content.SyncableContentProvider;
import android.provider.Contacts;

public class ContactsSyncAdapterService extends Service {
    private ContentProviderClient mContentProviderClient = null;

    public void onCreate() {
        mContentProviderClient =
                getContentResolver().acquireContentProviderClient(Contacts.CONTENT_URI);
    }

    public void onDestroy() {
        mContentProviderClient.release();
    }

    public IBinder onBind(Intent intent) {
        ContentProvider contentProvider = mContentProviderClient.getLocalContentProvider();
        if (contentProvider == null) throw new IllegalStateException();
        SyncableContentProvider syncableContentProvider = (SyncableContentProvider)contentProvider;
        return syncableContentProvider.getTempProviderSyncAdapter().getISyncAdapter().asBinder();
    }
}
