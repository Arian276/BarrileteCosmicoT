package com.barriletecosmicotv.navigation

object Screens {
    const val START = "start"   // 👈 agregado para que compile NavGraph
    const val LOGIN = "login"
    const val HOME = "home"
    const val STREAM = "stream"
    const val SEARCH = "search"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    fun streamWithId(streamId: String) = "$STREAM/$streamId"
}