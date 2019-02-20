package com.project.horselai.bindprogressguarddemo;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteService extends Service {
    private static final String TAG = "RemoteService";
    ExecutorService mExecutorService = Executors.newCachedThreadPool();

    RemoteCallbackList<IMyAidlInterfaceCallback> mRemoteCallbackList = new RemoteCallbackList<>();
    IMyAidlInterface.Stub myAidlInterface = new IMyAidlInterface.Stub() {
        @Override
        public void sendMessage(String msg) throws RemoteException {
            Log.i(TAG, "sendMessage: " + msg);
        }

        @Override
        public int getProcessId() throws RemoteException {
            return Process.myPid();
        }

        @Override
        public void registerCallback(IMyAidlInterfaceCallback callback) {
//            if (callback == null) return;
            Log.i(TAG, "registerCallback: ");
            mRemoteCallbackList.register(callback);
        }

        @Override
        public void unregisterCallback(IMyAidlInterfaceCallback callback) throws RemoteException {
//            if (callback == null) return;
            Log.i(TAG, "unregisterCallback: ");
            mRemoteCallbackList.unregister(callback);
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: ");
        return myAidlInterface;
    }


    boolean mLoop = true;
    int mCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: ");

        // 使用 AIDL 进行进程间通信
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                while (mLoop) {
                    final int registeredCallbackCount = mRemoteCallbackList.beginBroadcast();
//                    Log.i(TAG, "run: registeredCallbackCount:: " + registeredCallbackCount);
                    for (int i = 0; i < registeredCallbackCount; i++) {
                        IMyAidlInterfaceCallback callbackItem = mRemoteCallbackList.getBroadcastItem(i);
                        try {
                            Thread.sleep(1000);
                            callbackItem.onValueCallback(++mCount);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mRemoteCallbackList.finishBroadcast();
//                    Log.i(TAG, "run: " + mCount);
                }
            }
        });


        // 2. 使用 Socket 进行进程间通信
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    ServerSocket serverSocket = new ServerSocket();
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 6000);
                    serverSocket.bind(inetSocketAddress);
                    Log.i(TAG, "server waiting accept.. " + serverSocket.getInetAddress());
                    while (mLoop) {
                        Socket client = serverSocket.accept();
                        Log.i(TAG, "server accepted:: " + client.getRemoteSocketAddress());
                        mExecutorService.execute(new ClientHandler(client));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: ");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
        mLoop = false;
        mCount = 0;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged: ");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind: ");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(TAG, "onRebind: ");
    }


    class ClientHandler implements Runnable {

        Socket client;

        public ClientHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try (
                    BufferedInputStream bis = new BufferedInputStream(client.getInputStream());
                    BufferedOutputStream bos = new BufferedOutputStream(client.getOutputStream());
            ) {
//                byte[] bytes = new byte[256];
                int read = 0;
                while (mLoop) {
                    if (bis.available() <= 0) {
                        Thread.sleep(500L);
                        continue;
                    }
                    byte[] bytes = new byte[bis.available()];
                    read = bis.read(bytes, 0, bis.available());
                    String result = new String(bytes, 0, read);
                    Log.i(TAG, "server received: " + result);

                    bos.write(String.format("from server :: %s", result).getBytes());
                    bos.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {

                try {
                    if (client != null)
                        client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
