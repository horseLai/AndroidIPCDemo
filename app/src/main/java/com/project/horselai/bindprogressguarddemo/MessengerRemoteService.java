package com.project.horselai.bindprogressguarddemo;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class MessengerRemoteService extends Service {

    private static final String TAG = "MessengerRemoteService";
    private Messenger mMessenger;
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 3. 使用 Messenger 进行进程间通信
        mMessenger = new Messenger(new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.i(TAG, "handleMessage: " + msg);
                Log.i(TAG, "handleMessage data: " + msg.getData().get("msg"));

                Message message = Message.obtain();
                message.replyTo = mMessenger;
                Bundle bundle = new Bundle();
                bundle.putString("msg", "MSG from MessengerRemoteService..");
                message.setData(bundle);
                message.what = 124;
                try {
                    msg.replyTo.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }));
    }


}
