package com.project.horselai.bindprogressguarddemo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView textView;
    private Button btnBindRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        btnBindRemote = findViewById(R.id.btnBindRemote);

    }


    private IMyAidlInterface mRemoteStub;
    private IMyAidlInterface mRemoteStub2;
    IPCClientConnection mIpcClientConnection;
    ExecutorService mExecutorService = Executors.newCachedThreadPool();
    private boolean mIsBond = false;
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected: ");
            mRemoteStub = IMyAidlInterface.Stub.asInterface(service);
            try {
                mRemoteStub.registerCallback(myAidlInterfaceCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mIpcClientConnection = new IPCClientConnection();
            mExecutorService.execute(mIpcClientConnection);
            btnBindRemote.setEnabled(false);
            mIsBond = true;
            Toast.makeText(MainActivity.this, "service bond!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected: ");
            if (mRemoteStub != null) {
                try {
                    mRemoteStub.sendMessage("onServiceDisconnected from Main");
                    mRemoteStub.unregisterCallback(myAidlInterfaceCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            mIsBond = false;
            btnBindRemote.setEnabled(true);
        }
    };

    ServiceConnection mServiceConnection2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected2: ");
            mRemoteStub2 = IMyAidlInterface.Stub.asInterface(service);
            try {
                mRemoteStub2.registerCallback(myAidlInterfaceCallback2);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            btnBindRemote.setEnabled(false);
            mIsBond = true;
            Toast.makeText(MainActivity.this, "service bond 2!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "onServiceDisconnected 2: ");
            if (mRemoteStub != null) {
                try {
                    mRemoteStub2.sendMessage("onServiceDisconnected 2 from Main");
                    mRemoteStub2.unregisterCallback(myAidlInterfaceCallback2);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            mIsBond = false;
            btnBindRemote.setEnabled(true);
        }
    };


    public void sendToRemoteService(View view) {
        if (mRemoteStub == null) {
            textView.setText("mRemoteStub is null");
            return;
        }
        try {
            mRemoteStub.sendMessage("clicked from Main");
            mRemoteStub.sendMessageObj(new MyMessage(12, "kjergjkergjker"));
            textView.setText(String.format("process id: %s", mRemoteStub.getProcessId()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                mIpcClientConnection.sendMessage("hello " + Math.random() * 1000);
            }
        });
    }

    public void bindRemoteService(View view) {
        Intent intent = new Intent(this, RemoteService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        bindService(intent, mServiceConnection2, BIND_AUTO_CREATE);

    }

    public void unbindRemoteService(View view) {
        if (!mIsBond) {
            Toast.makeText(this, "service not bond!", Toast.LENGTH_SHORT).show();
            return;
        }
        mIsBond = false;
        btnBindRemote.setEnabled(true);
        unbindService(mServiceConnection);
        unbindService(mServiceConnection2);
        mIpcClientConnection.close();
    }

    IMyAidlInterfaceCallback myAidlInterfaceCallback = new IMyAidlInterfaceCallback.Stub() {
        @Override
        public void onValueCallback(int value) throws RemoteException {
//            Log.i(TAG, "onValueCallback: " + value);
            mHandler.obtainMessage(1, "onValueCallback: " + value).sendToTarget();
        }
    };

    IMyAidlInterfaceCallback myAidlInterfaceCallback2 = new IMyAidlInterfaceCallback.Stub() {
        @Override
        public void onValueCallback(int value) throws RemoteException {
//            Log.i(TAG, "onValueCallback2: " + value);
            mHandler.obtainMessage(1, "onValueCallback2: " + value).sendToTarget();
        }
    };

    Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 2) {
                Toast.makeText(MainActivity.this, "" + msg.obj, Toast.LENGTH_SHORT).show();
                return true;
            }
            textView.setText(String.valueOf(msg.obj));
            return true;
        }
    });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        unbindRemoteService(null);
        mExecutorService.shutdownNow();
    }


    //
    class IPCClientConnection implements Runnable {

        private BufferedOutputStream bos;
        private Socket client;
        private boolean mLoop = true;


        void sendMessage(String msg) {
            if (bos == null || client == null || client.isClosed() || !client.isConnected()) {
//                throw new IllegalStateException("socket client is not available..");
                return;
            }
            try {
                bos.write(msg.getBytes());
                bos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void close() {
            mLoop = false;
        }

        @Override
        public void run() {
            try {
                client = new Socket(InetAddress.getLocalHost(), 6000);
                client.setKeepAlive(true);
                bos = new BufferedOutputStream(client.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            try (
                    BufferedInputStream bis = new BufferedInputStream(client.getInputStream());
            ) {
                int read = 0;
                byte[] bytes = new byte[256];
                while (mLoop) {

                    if (bis.available() <= 0) {
                        Thread.sleep(500L);
                        continue;
                    }
                    if (bytes.length < bis.available())
                        bytes = new byte[bis.available()];
                    read = bis.read(bytes, 0, bis.available());

                    String result = new String(bytes, 0, read);
                    Log.i(TAG, "client received:: " + result);
                    mHandler.obtainMessage(2, "client received:: " + result).sendToTarget();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bos != null)
                        bos.close();
                    if (client != null)
                        client.close();
                    client = null;
                    bos = null;
                } catch (IOException e) {
                    // ...
                }
            }
        }


    }
}
