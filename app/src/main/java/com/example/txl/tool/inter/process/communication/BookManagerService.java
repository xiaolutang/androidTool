package com.example.txl.tool.inter.process.communication;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BookManagerService extends Service {
    private static final String TAG = "BookManagerService";

    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<>(  );

    private Binder mBinder = new IBookManager.Stub() {
        @Override
        public List<Book> getBookList() throws RemoteException {
            Log.d( TAG,"getBookList thread :"+Thread.currentThread().getName() );
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            Log.d( TAG,"addBook thread :"+Thread.currentThread().getName() );
            mBookList.add( book );
        }
    };

    public BookManagerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add( new Book( 1,"Android","小王" ) );
        mBookList.add( new Book( 2,"IOS" ,"小张") );
    }

    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }
}
