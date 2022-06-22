package com.example.AnythingGroup.fragments.title;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

// Содержит основные данные тайла
// Изображение (постер) и название могут отсутствовать
// и загружаться во время полной загрузки тайтла
public class TitleMain implements Parcelable {
    public String reference;
    public String name;
    public Bitmap image;

    public TitleMain(){

    }

    protected TitleMain(Parcel in) {
        reference = in.readString();
        name = in.readString();
        image = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Creator<TitleMain> CREATOR = new Creator<TitleMain>() {
        @Override
        public TitleMain createFromParcel(Parcel in) {
            return new TitleMain(in);
        }

        @Override
        public TitleMain[] newArray(int size) {
            return new TitleMain[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(reference);
        parcel.writeString(name);
        parcel.writeParcelable(image, i);
    }
}
