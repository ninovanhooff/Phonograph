package com.dimowner.phonograph;

import android.content.Context;

import com.dimowner.phonograph.audio.player.AudioPlayer;
import com.dimowner.phonograph.audio.player.PlayerContract;
import com.dimowner.phonograph.data.FileRepository;
import com.dimowner.phonograph.data.FileRepositoryImpl;
import com.dimowner.phonograph.data.PhonographPrefs;
import com.dimowner.phonograph.data.PhonographPrefsImpl;

class PhonographInjector {

    private final Context context;
    private PhonographColorMap colorMap;

    PhonographInjector(Context applicationContext) {
        this.context = applicationContext;
        colorMap = new PhonographColorMap() {
            @Override
            public int getPrimaryColorRes() {
                return R.color.md_blue_700;
            }
        };
    }

    public PhonographColorMap provideColorMap() {
        return colorMap;
    }

    PhonographPrefs providePrefs() {
        return PhonographPrefsImpl.getInstance(context);
    }

    public FileRepository provideFileRepository() {
        return FileRepositoryImpl.getInstance(context, providePrefs());
    }

    public PlayerContract.Player provideAudioPlayer() {
        return AudioPlayer.getInstance();
    }


}
