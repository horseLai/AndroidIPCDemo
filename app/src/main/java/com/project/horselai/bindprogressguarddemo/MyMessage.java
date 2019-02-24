package com.project.horselai.bindprogressguarddemo;

import android.os.Parcel;
import android.os.Parcelable;

public class MyMessage implements Parcelable {

    public int what;
    public String msg;

    public MyMessage(int what, String msg) {
        this.what = what;
        this.msg = msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MyMessage{");
        sb.append("what=").append(what);
        sb.append(", msg='").append(msg).append('\'');
        sb.append('}');
        return sb.toString();
    }

    protected MyMessage(Parcel in) {
        what = in.readInt();
        msg = in.readString();
    }

    public static final Creator<MyMessage> CREATOR = new Creator<MyMessage>() {
        @Override
        public MyMessage createFromParcel(Parcel in) {
            return new MyMessage(in);
        }

        @Override
        public MyMessage[] newArray(int size) {
            return new MyMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(what);
        dest.writeString(msg);
    }
}
