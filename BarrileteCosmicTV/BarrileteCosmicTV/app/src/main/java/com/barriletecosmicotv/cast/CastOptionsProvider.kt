package com.barriletecosmicotv.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.CastMediaControlIntent

public class CastOptionsProvider : OptionsProvider {
    
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setLaunchOptions(
                LaunchOptions.Builder()
                    .setAndroidReceiverCompatible(true)
                    .build()
            )
            .setStopReceiverApplicationWhenEndingSession(true)
            .setResumeSavedSession(true)
            .build()
    }
    
    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}