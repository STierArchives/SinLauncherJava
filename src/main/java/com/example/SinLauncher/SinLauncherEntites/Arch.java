package com.example.SinLauncher.SinLauncherEntites;

import com.google.gson.annotations.SerializedName;

public enum Arch {
    @SerializedName("x86")
    X86,

    @SerializedName("x86_64")
    X86_64,

    @SerializedName("arm64")
    Arm64,

    @SerializedName("arm")
    Arm,
}
