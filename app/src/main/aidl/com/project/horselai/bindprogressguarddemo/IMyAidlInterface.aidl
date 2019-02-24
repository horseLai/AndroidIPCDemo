// IMyAidlInterface.aidl
package com.project.horselai.bindprogressguarddemo;

import com.project.horselai.bindprogressguarddemo.IMyAidlInterfaceCallback;
import com.project.horselai.bindprogressguarddemo.MyMessage;

interface IMyAidlInterface {

    void sendMessage(String msg);

    void sendMessageObj(in MyMessage msg);

    int getProcessId();

    void registerCallback(IMyAidlInterfaceCallback callback);

    void unregisterCallback(IMyAidlInterfaceCallback callback);

}
