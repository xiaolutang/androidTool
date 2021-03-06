package com.txl.lib.image_load;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import androidx.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.example.txl.tool.R;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copyright (c) 2018, 唐小陆 All rights reserved.
 * author：txl
 * date：2018/8/6
 * description：bitmap加载和缓存
 */
public class ImageLoader {
    private static final String TAG = "ImageLoader";

    public static final int MESSAGE_POST_RESULT = 1;
    public static final int MESSAGE_POST_IMAGE_SIZE = 2;
    public static final int MESSAGE_POST_BITMAP = 3;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private static final int CORE_POOL_SIZE= CPU_COUNT +1;

    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2+1;

    private static final long KEEP_ALIVE = 10L;

    private static final int TAG_KEY_URI = R.id.image_item;//fixme R.id.imageloader_uri

    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;//文件缓存大小为50M

    private static final int IO_BUFFER_SIZE = 8 * 1024;

    private static final int DISK_CACHE_INDEX = 0;

    private boolean mIsDiskLruCacheCreate = false;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread( r, "ImageLoader#"+mCount.getAndIncrement() );
        }
    };

    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor( CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(  ), sThreadFactory );

    private Handler mMainHandler = new Handler( Looper.getMainLooper() ){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_POST_RESULT:
                    LoaderResult result = (LoaderResult) msg.obj;
                    ImageView imageView = result.imageView;
                    String uri = (String) imageView.getTag( TAG_KEY_URI );
                    if(uri.equals( result.uri )){
                        imageView.setImageBitmap( result.bitmap );
                    }else {
                        Log.w( TAG,"set image bitmap, but url has changed, ignored!" );
                    }
                    return;
                case MESSAGE_POST_IMAGE_SIZE:
                    SourceReady ready = (SourceReady) msg.obj;
                    ready.bitmapSourceReady(msg.arg1, msg.arg2 );
                    return;
                case MESSAGE_POST_BITMAP:
                    LoadBitmapCallBack callBack = (LoadBitmapCallBack) msg.obj;
                    callBack.onFinish(callBack.b);
                    return;
            }

            super.handleMessage( msg );
        }
    };

    private Context mContext;
    private ImageResizer mImageResizer = new ImageResizer();
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;

    private ImageLoader(Context context){
        mContext = context;
        int maxMemory = (int) (Runtime.getRuntime().maxMemory()/1024);
        int cacheSize = maxMemory/8;
        mMemoryCache = new LruCache<String, Bitmap>( cacheSize ){
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
        if(!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }
        long m = getUsableSpace(diskCacheDir);
        Log.d( TAG," m="+m+" hh="+ DISK_CACHE_SIZE+"  "+(m > DISK_CACHE_SIZE));
        if(m > DISK_CACHE_SIZE){
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir,1,1,DISK_CACHE_SIZE);
                mIsDiskLruCacheCreate = true;
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static ImageLoader build(Context context){
        Log.d( TAG, "build");
        return new ImageLoader( context );
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap){
        if(getBitmapFromMemCache(key) == null){
            mMemoryCache.put( key,bitmap );
        }
    }

    private Bitmap getBitmapFromMemCache(String key){
        return mMemoryCache.get( key );
    }

    /**
     * load bitmap from memory cache or disk cache or network async,
     * then bind ImageView and bitmap
     * Note THAT: should run in UI Thread
     * @param uri http url
     * @param imageView bitmap's bind Object
     * */
    private void bindBitmap(final String uri, final ImageView imageView){
        bindBitmap( uri,imageView,0,0 );
    }

    public void bindBitmap(final String uri, final ImageView imageView, final int reqWidth, final int reqHeight){
        imageView.setTag(TAG_KEY_URI,uri );
        Bitmap bitmap = loadBitmapFromMemCache(uri);
        if(bitmap != null){
            imageView.setImageBitmap( bitmap );
            Log.d( TAG,"loadBitmapFromMemCache,uri:"+uri);
            return;
        }
        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(uri,reqWidth,reqHeight);
                if(bitmap != null){
                    LoaderResult result = new LoaderResult(imageView,uri,bitmap);
                    mMainHandler.sendMessage(  mMainHandler.obtainMessage(MESSAGE_POST_RESULT,result) );
//                    sendToTarget();
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute( loadBitmapTask );
    }

    /**
     * load bitmap from memory cache or disk cache or network
     * @param uri http url
     * @param reqWidth the width ImageView desired
     * @param reqHeight the height ImageView desired
     * @return bitmap, maybe null
     * */
    public Bitmap loadBitmap(String uri, int reqWidth, int reqHeight){
        Bitmap bitmap = loadBitmapFromMemCache(uri);
        if(bitmap != null){
            Log.d( TAG,"loadBitmapFromMemCache,uri:"+uri );
            return bitmap;
        }
        try {
            bitmap = loadBitmapFromDiskCache(uri,reqWidth,reqHeight);
            if(bitmap != null){
                Log.d( TAG,"loadBitmapFromDiskCache,uri:"+uri );
                return bitmap;
            }
            bitmap = loadBitmapFromHttp( uri,reqWidth,reqHeight );
        }catch (IOException e){
            e.printStackTrace();
        }
        if(bitmap == null && !mIsDiskLruCacheCreate){
            Log.w( TAG,"encounter error, DiskLruCache is not Create."+uri );
            bitmap = downloadBitmapFromUrl(uri);
        }
        return bitmap;
    }

    /**
     * 从缓存中加载bitmap
     * */
    public void loadBitmapFromCache(final String uri, final int reqWidth, final int reqHeight, final LoadBitmapCallBack callBack){
        final Bitmap[] bitmap = {loadBitmapFromMemCache(uri)};
        if(bitmap[0] != null && callBack != null){
            Log.d( TAG,"loadBitmapFromCache,uri:"+uri );
            callBack.onFinish(bitmap[0]);
            return;
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    bitmap[0] = loadBitmapFromDiskCache(uri,reqWidth,reqHeight);
                    Log.d( TAG,"loadBitmapFromCache loadBitmapFromDiskCache ,uri:"+uri );
                    callBack.b = bitmap[0];
                    mMainHandler.sendMessage(  mMainHandler.obtainMessage(MESSAGE_POST_BITMAP,callBack) );
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute( task );
    }

    private Bitmap loadBitmapFromMemCache(String url){
        final String key = hashKeyFromUrl(url);
        Bitmap bitmap = getBitmapFromMemCache( key );
        return bitmap;
    }

    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException{
        if(Looper.myLooper() == Looper.getMainLooper()){
            throw new RuntimeException( "can not visit network from UI Thread" );
        }
        if(mDiskLruCache == null){
            return null;
        }
        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if(editor != null){
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if(downloadUrlToStream(url,outputStream)){
                editor.commit();
            }else {
                editor.abort();
            }
            mDiskLruCache.flush();
        }
        Log.d( TAG,"loadBitmapFromHttp DiskLruCache loadBitmapFromHttp,uri:"+url +"reqWidth,"+reqWidth+"reqWidth"  +reqWidth);
        loadBitmapFromDiskCache(url,reqWidth,reqHeight);
        return loadBitmapFromMemCache( url );
    }

    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException{
        if(Looper.myLooper() == Looper.getMainLooper()){
            Log.w(TAG,"load bitmap from UI Thread, it's not recommended!");
        }
        if(mDiskLruCache == null){
            return null;
        }
        Bitmap bitmap = null;
        String key = hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if(snapshot != null){
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream( DISK_CACHE_INDEX );
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = mImageResizer.decodeSampledBitmapFromFileDescriptor(fileDescriptor,reqWidth, reqHeight);
            if(bitmap != null){
                addBitmapToMemoryCache( key,bitmap );
            }
        }
        return bitmap;
    }

    public boolean downloadUrlToStream(String urlString, OutputStream outputStream){
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL( urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream( urlConnection.getInputStream(),IO_BUFFER_SIZE );
            out = new BufferedOutputStream( outputStream,IO_BUFFER_SIZE );
            int b;
            while ((b = in.read()) != -1){
                out.write( b );
            }
            out.close();
            in.close();
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
//            MyUtils.close(out);
//            MyUtils.close(in);
        }
        return false;
    }

    private Bitmap downloadBitmapFromUrl(String urlString){
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL( urlString );
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream( urlConnection.getInputStream(), IO_BUFFER_SIZE );
            bitmap = BitmapFactory.decodeStream( in );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(urlConnection != null){
                urlConnection.disconnect();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    private String hashKeyFromUrl(String url){
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance( "MD5" );
            mDigest.update( url.getBytes() );
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            cacheKey = String.valueOf( url.hashCode() );
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder(  );
        for (int i=0; i<bytes.length; i++){
            String hex = Integer.toHexString( 0xFF & bytes[i] );
            if(hex.length() == 1){
                sb.append( "0" );
            }
            sb.append( hex );
        }
        return sb.toString();
    }

    public File getDiskCacheDir(Context context, String uniqueName){
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED );
        final String cachePath;
        if(externalStorageAvailable){
            cachePath = context.getExternalCacheDir().getPath();
        }else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File( cachePath+File.separator+uniqueName );
    }

    private long getUsableSpace(File path){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
            return path.getUsableSpace();
        }
        final StatFs statFs = new StatFs( path.getPath() );
        return statFs.getBlockSizeLong() + statFs.getAvailableBlocksLong();
    }

    public void decodeBitmapSize(final String urlString, final SourceReady sourceReady){
        Runnable ImageSizeTask = new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try {
                    URL url = new URL( urlString );
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    BitmapFactory.decodeStream( connection.getInputStream(),null,options );
                    Message message = mMainHandler.obtainMessage( MESSAGE_POST_IMAGE_SIZE,sourceReady);
                    message.arg1 = options.outWidth;
                    message.arg2 = options.outHeight;
                    mMainHandler.sendMessage( message );
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute( ImageSizeTask );
    }

    private static class LoaderResult{
        public ImageView imageView;
        public String uri;
        public Bitmap bitmap;

        public LoaderResult(ImageView imageView, String uri, Bitmap bitmap) {
            this.imageView = imageView;
            this.uri = uri;
            this.bitmap = bitmap;
        }
    }

    public interface SourceReady{
        void bitmapSourceReady(int bitmapWidth, int bitmapHeight);
    }

    public static abstract class LoadBitmapCallBack{
        Bitmap b = null;
        public abstract void onFinish(Bitmap bitmap);
    }
}
