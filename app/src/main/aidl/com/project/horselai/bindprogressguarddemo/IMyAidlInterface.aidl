// IMyAidlInterface.aidl
package com.project.horselai.bindprogressguarddemo;

import com.project.horselai.bindprogressguarddemo.IMyAidlInterfaceCallback;

interface IMyAidlInterface {

    void sendMessage(String msg);

    int getProcessId();

    void registerCallback(IMyAidlInterfaceCallback callback);

    void unregisterCallback(IMyAidlInterfaceCallback callback);

}
