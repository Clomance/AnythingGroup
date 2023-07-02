package com.example.AnythingGroup.fragments.video;

public class VideoSource {
    public VideoSourceType type;

    public String data;

    public VideoSource(VideoSourceType type, String data){
        this.type = type;
        this.data = data;
    }

    public enum VideoSourceType{
        StreamingUrl,
        Iframe
    }
}