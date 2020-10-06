package com.graduate.roa.network;

import com.google.gson.annotations.SerializedName;

import java.io.File;

public class WavFile {
    @SerializedName("name")
    public String name;
    @SerializedName("file")
    public File file;

    public WavFile(String name, File file) {
        this.name = name;
        this.file = file;
    }
}
