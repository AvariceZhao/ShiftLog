package com.clockin.app.update

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val pageUrl: String,
    /** GitHub Release 附件中的 APK 直链 */
    val apkDownloadUrl: String? = null,
    val apkSizeBytes: Long? = null,
) {
    val hasInAppApk: Boolean get() = !apkDownloadUrl.isNullOrBlank()
}
