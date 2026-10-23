package org.aquamarine5.brainspark.chaoxingsignfaker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LocalGitHubUpdateManager = staticCompositionLocalOf<GitHubUpdateManager> { error("Missing update host") }

@Composable
fun GitHubUpdateHost(manager: GitHubUpdateManager) {
    LaunchedEffect(manager) { manager.check() }
    manager.release?.let { release ->
        AlertDialog(onDismissRequest = manager::dismiss,
            title = { Text("发现新版本 ${release.tag}") },
            text = {
                Text(release.notes.ifBlank { "使用默认浏览器下载最新版本。" },
                    modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()))
            },
            confirmButton = {
                TextButton(onClick = manager::openUpdateInBrowser) { Text("浏览器下载更新") }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = manager::ignore) { Text("忽略此次更新") }
                    TextButton(onClick = manager::dismiss) { Text("稍后再说") }
                }
            })
    }
}

@Composable
fun GitHubUpdateButton() {
    val manager = LocalGitHubUpdateManager.current
    Column {
        Text(manager.status)
        OutlinedButton(onClick = { manager.check(isManual = true) },
            enabled = !manager.isChecking, modifier = Modifier.fillMaxWidth()) {
            Text(if (manager.isChecking) "正在检查…" else "检查 GitHub 更新")
        }
    }
}
