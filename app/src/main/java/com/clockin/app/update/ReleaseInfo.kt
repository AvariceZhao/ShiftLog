package com.clockin.app.update

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val pageUrl: String,
)
