package com.ninovanhooff.phonograph;

import android.content.Context;

import com.dimowner.phonograph.R;
import com.ninovanhooff.phonograph.audio.player.AudioPlayer;
import com.ninovanhooff.phonograph.audio.player.PlayerContract;
import com.ninovanhooff.phonograph.data.FileRepository;
import com.ninovanhooff.phonograph.data.FileRepositoryImpl;
import com.ninovanhooff.phonograph.data.PhonographPrefs;
import com.ninovanhooff.phonograph.data.PhonographPrefsImpl;

import androidx.annotation.Nullable;

class PhonographInjector {

    private final Context context;
    private final PhonographColorMap colorMap;
    private final PhonographPrefs prefs;

    PhonographInjector(Context applicationContext, @Nullable PhonographPrefs prefs) {
        this.context = applicationContext;
        colorMap = new PhonographColorMap() {
            @Override
            public int getPrimaryColorRes() {
                return R.color.md_blue_700;
            }
        };
        if (prefs == null){
            this.prefs = PhonographPrefsImpl.getInstance(context);
        } else {
            this.prefs = prefs;
        }

    }

    public PhonographColorMap provideColorMap() {
        return colorMap;
    }

    PhonographPrefs providePrefs() {
        return prefs;
    }

    public FileRepository provideFileRepository() {
        return FileRepositoryImpl.getInstance(context, providePrefs());
    }

    public PlayerContract.Player provideAudioPlayer() {
        return AudioPlayer.getInstance();
    }


}
