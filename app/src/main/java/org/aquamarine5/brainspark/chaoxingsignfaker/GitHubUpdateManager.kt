package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkPredictable
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.util.concurrent.TimeUnit

data class GitHubRelease(val tag: String, val notes: String, val url: String)

class GitHubUpdateManager(private val activity: ComponentActivity) {
    var status by mutableStateOf("当前版本 ${BuildConfig.VERSION_NAME}")
        private set
    var release by mutableStateOf<GitHubRelease?>(null)
        private set
    var isUpdateAvailable by mutableStateOf(false)
        private set
    var isChecking by mutableStateOf(false)
        private set
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()

    fun check(isManual: Boolean = false) {
        if (isChecking) return
        if (!isManual && isAutomaticCheckDone) return
        isAutomaticCheckDone = true
        isChecking = true
        activity.lifecycleScope.launch {
            runCatching {
                status = "正在检查 GitHub 更新…"
                val latest = withContext(Dispatchers.IO) { fetch() }
                val comparison = compareVersions(latest.tag, BuildConfig.VERSION_NAME)
                checkPredictable(comparison != null) { "无法比较版本 ${latest.tag}，请检查发布标签" }
                if (comparison <= 0) {
                    isUpdateAvailable = false
                    status = "已是最新版本 · ${BuildConfig.VERSION_NAME}"
                    return@runCatching
                }
                isUpdateAvailable = true
                val ignored = activity.chaoxingDataStore.data.first().ignoredReleaseTag
                if (isManual || latest.tag != ignored) {
                    release = latest
                    status = "发现新版本 · ${latest.tag}"
                } else {
                    isUpdateAvailable = false
                    status = "已忽略 ${latest.tag} · 可手动检查更新"
                }
            }.onFailure { reportFailure(it) }
            isChecking = false
        }
    }

    private fun fetch(): GitHubRelease {
        val request = Request.Builder().url("https://api.github.com/repos/rD227/ChaoxingSignFaker/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ChaoxingSignFaker-rD227/${BuildConfig.VERSION_NAME}").build()
        return client.newCall(request).execute().use { response ->
            checkPredictable(response.code != 404) { "暂无正式 Release，发布后即可检查更新" }
            checkPredictable(response.code != 403 && response.code != 429) { "GitHub 请求受限，请稍后重试" }
            checkPredictable(response.isSuccessful) { "检查更新失败：HTTP ${response.code}" }
            val bytes = response.body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    checkPredictable(output.size() + count <= MAX_RELEASE_BYTES) { "Release 响应过大" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val json = JSONObject(bytes.toString(Charsets.UTF_8))
            checkPredictable(!json.getBoolean("draft") && !json.getBoolean("prerelease")) { "暂无正式 Release" }
            val assets = json.getJSONArray("assets")
            val candidates = (0 until assets.length()).map { assets.getJSONObject(it) }
            val asset = candidates.firstOrNull { it.getString("name") == "app-release.apk" }
                ?: candidates.firstOrNull { it.getString("name") == "app-release-unsigned.apk" }
            checkPredictable(asset != null && asset.getString("state") == "uploaded") {
                "最新 Release 未包含默认构建名称的 APK"
            }
            val url = asset.getString("browser_download_url")
            val uri = url.toUri()
            checkPredictable(uri.scheme == "https" && uri.host == "github.com" &&
                uri.path.orEmpty().startsWith("/rD227/ChaoxingSignFaker/releases/download/")) { "更新附件地址无效" }
            GitHubRelease(json.getString("tag_name"), json.optString("body"), url)
        }
    }

    fun dismiss() { release = null }

    fun ignore() {
        val tag = release?.tag ?: return
        activity.lifecycleScope.launch {
            runCatching {
                activity.chaoxingDataStore.updateData { it.toBuilder().setIgnoredReleaseTag(tag).build() }
                release = null
                status = "已忽略 $tag"
                isUpdateAvailable = false
            }.onFailure { reportFailure(it) }
        }
    }

    fun openUpdateInBrowser() {
        val latest = release ?: return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, latest.url.toUri()).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                selector = Intent(Intent.ACTION_VIEW, "https://".toUri())
                    .addCategory(Intent.CATEGORY_BROWSABLE)
            }
            activity.startActivity(intent)
            release = null
            status = "已在默认浏览器打开 ${latest.tag} 下载链接"
        }.onFailure { error ->
            reportFailure(error)
            Toast.makeText(activity, status, Toast.LENGTH_LONG).show()
        }
    }

    private fun reportFailure(error: Throwable) {
        if (error is CancellationException) throw error
        status = if (error is ActivityNotFoundException) {
            "未找到可用浏览器，请先安装或启用浏览器后重试"
        } else {
            error.message ?: "更新操作失败，请检查网络后重试"
        }
        error.printStackTrace()
    }

    fun close() {
        client.dispatcher.cancelAll()
        if (isChecking) isAutomaticCheckDone = false
    }

    companion object {
        private const val MAX_RELEASE_BYTES = 1024 * 1024
        private var isAutomaticCheckDone = false

        fun compareVersions(left: String, right: String): Int? {
            fun parts(value: String): Pair<List<BigInteger>, String>? {
                val match = Regex("[vV]?([0-9]+(?:\\.[0-9]+)*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?")
                    .matchEntire(value.trim()) ?: return null
                return match.groupValues[1].split('.').map { it.toBigInteger() } to match.groupValues[2]
            }
            val a = parts(left) ?: return null
            val b = parts(right) ?: return null
            for (index in 0 until maxOf(a.first.size, b.first.size)) {
                val diff = (a.first.getOrNull(index) ?: BigInteger.ZERO)
                    .compareTo(b.first.getOrNull(index) ?: BigInteger.ZERO)
                if (diff != 0) return diff
            }
            if (a.second == b.second) return 0
            if (a.second.isEmpty()) return 1
            if (b.second.isEmpty()) return -1
            val aa = a.second.split('.'); val bb = b.second.split('.')
            for (index in 0 until minOf(aa.size, bb.size)) {
                val x = aa[index]; val y = bb[index]
                val xn = x.toBigIntegerOrNull(); val yn = y.toBigIntegerOrNull()
                val diff = when {
                    xn != null && yn != null -> xn.compareTo(yn)
                    xn != null -> -1
                    yn != null -> 1
                    else -> x.compareTo(y)
                }
                if (diff != 0) return diff
            }
            return aa.size.compareTo(bb.size)
        }
    }
}
