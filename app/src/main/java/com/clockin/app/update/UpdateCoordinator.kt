package com.clockin.app.update

import android.content.Context
import android.os.Build
import com.clockin.app.data.UpdateDismissStore
import com.clockin.app.domain.VersionComparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val release: ReleaseInfo) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

data class AppVersion(
    val versionName: String,
    val versionCode: Long,
) {
    companion object {
        fun current(context: Context): AppVersion {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }
            return AppVersion(
                versionName = info.versionName ?: "0",
                versionCode = code,
            )
        }
    }
}

class UpdateCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val dismissStore = UpdateDismissStore(appContext)

    private val _promptRelease = MutableStateFlow<ReleaseInfo?>(null)
    val promptRelease: StateFlow<ReleaseInfo?> = _promptRelease.asStateFlow()

    suspend fun checkForUpdate(manual: Boolean): UpdateCheckResult {
        if (!manual && !dismissStore.shouldRunAutoCheck()) {
            return UpdateCheckResult.UpToDate
        }
        val current = AppVersion.current(appContext)
        val release = GitHubUpdateChecker.fetchLatestRelease().getOrElse { error ->
            return UpdateCheckResult.Failed(
                error.message?.let { "检查失败：$it" } ?: "检查失败，请稍后重试",
            )
        }
        if (!VersionComparator.isNewer(release.versionName, current.versionName)) {
            if (!manual) dismissStore.markAutoCheckRan()
            return UpdateCheckResult.UpToDate
        }
        if (!manual) {
            dismissStore.markAutoCheckRan()
            if (dismissStore.getDismissedTag() == release.tagName) {
                return UpdateCheckResult.UpToDate
            }
            _promptRelease.value = release
            return UpdateCheckResult.UpToDate
        }
        return UpdateCheckResult.Available(release)
    }

    suspend fun dismissRelease(tagName: String) {
        dismissStore.setDismissedTag(tagName)
        clearPrompt()
    }

    fun clearPrompt() {
        _promptRelease.value = null
    }
}
