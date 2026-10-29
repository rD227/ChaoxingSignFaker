/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.ClipboardManager
import org.aquamarine5.brainspark.chaoxingsignfaker.GitHubUpdateButton
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.BuildConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CurrentDataStoreDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CustomizeClientCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SnackbarAlertDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.FontGilroy
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalImageLoader
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.OnlyAppDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.isDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.logLocally

@Serializable
object SettingGraphDestination

@Serializable
object SettingDestination


var isAlwaysForceSign by mutableStateOf(false)

var isAllowExternalQuerySettingTipsRequested by mutableStateOf(false)

@Composable
fun SettingScreen(
    naviToLoginScreen: () -> Unit,
    naviToFavoriteLocationSetting: () -> Unit = {}
) {
    val imageLoader = LocalImageLoader.current
    Column(
        modifier = Modifier
            .padding(16.dp, 4.dp, 16.dp, 0.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val context = LocalContext.current
        val hapticFeedback = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackbarHostState.current
        val userEntity = remember { ChaoxingHttpClient.instance!!.userEntity }
        val displayUserEntity =
            (ChaoxingHttpClient.cloneInstance ?: ChaoxingHttpClient.instance!!).userEntity
        var isShowSignoffDialog by remember { mutableStateOf(false) }
        var isIgnoreAllConsistentDeviceCodeComponents by remember { mutableStateOf(false) }
        var isAllowExternalProviderFetchInformation by remember { mutableStateOf(false) }
        var isExternalProviderInformationTipsDialogVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            context.chaoxingDataStore.data.first().apply {
                isAlwaysForceSign = preferences.alwaysForceSign
                isIgnoreAllConsistentDeviceCodeComponents =
                    preferences.isIgnoreAllConsistentDeviceCodeComponents
                isAllowExternalProviderFetchInformation =
                    preferences.isAllowExternalProviderFetchInformation
            }
            if (isAllowExternalQuerySettingTipsRequested) {
                isAllowExternalQuerySettingTipsRequested = false
                isExternalProviderInformationTipsDialogVisible = true
            }
        }

        GitHubUpdateButton()
        Spacer(modifier = Modifier.height(8.dp))
        if (isShowSignoffDialog) {
            SnackbarAlertDialog(
                onDismissRequest = { isShowSignoffDialog = false },
                title = { Text("确定要登出吗？") },
                text = {
                    Text("当你登出时，本机保存的代签用户不会丢失。")
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isShowSignoffDialog = false
                    }) {
                        Text("取消")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            coroutineScope.launch {
                                ChaoxingHttpClient.cloneInstance = null
                                context.chaoxingDataStore.updateData {
                                    it.toBuilder()
                                        .clearLoginSession()
                                        .build()
                                }
                                naviToLoginScreen()
                            }
                        }
                    ) {
                        Text("登出")
                    }
                }
            )
        }
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0ADA0))
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp, 8.dp)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    displayUserEntity.pic,
                    imageLoader = imageLoader,
                    contentDescription = "头像",
                    modifier = Modifier
                        .height(40.dp)
                        .width(40.dp)
                        .clip(
                            RoundedCornerShape(5.dp)
                        )
                )
                Text(
                    "登录用户：${displayUserEntity.name}",
                    modifier = Modifier
                        .padding(8.dp, 0.dp)
                        .weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color.Black else Color.White
                )
                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isShowSignoffDialog = true
                    }
                ) { Icon(painterResource(R.drawable.ic_log_out), null, tint = Color.White) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))


        Button(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                naviToFavoriteLocationSetting()
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7FB0DC))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_map_pinned),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "收藏的签到位置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                    Text(
                        "在地图上收藏常用的签到位置，位置签到时可以直接选用。",
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Spacer(modifier = Modifier.height(8.dp))
        CustomizeClientCard()
        Button(
            onClick = {
                runCatching {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/rD227/ChaoxingSignFaker".toUri()
                        )
                    )
                }.onFailure {
                    snackbarHostState.displaySnackbar("无法打开链接", coroutineScope)
                    it.logLocally()
                }
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF55BB8A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_github), contentDescription = "github")
                Spacer(modifier = Modifier.width(8.dp))
                Text(buildAnnotatedString {
                    append("查看本修改版源码 或者请赞赏原作者！\n前往：")
                    withStyle(
                        SpanStyle(
                            fontFamily = FontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("rD227")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("/")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = FontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("ChaoxingSignFaker")
                    }
                })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                runCatching {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    context.startActivity(Intent(Intent.ACTION_SEND).apply {
                        setData("mailto:aquamarine5forever@gmail.com".toUri())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_EMAIL, "aquamarine5forever@gmail.com")
                        putExtra(Intent.EXTRA_CC, "aquamarine5forever@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Send to ChaoxingSignFaker:\n")
                        putExtra(Intent.EXTRA_TEXT, "Your content:")
                    })
                }.onFailure {
                    snackbarHostState.displaySnackbar("无法打开邮件应用", coroutineScope)
                    it.logLocally()
                }
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC08EAF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_mail), contentDescription = "mail")
                Spacer(modifier = Modifier.width(8.dp))
                Text(buildAnnotatedString {
                    append("想要联系原作者？\n发送邮件到：")
                    withStyle(
                        SpanStyle(
                            fontFamily = FontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("aquamarine5forever")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("@")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = FontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("gmail.com")
                    }
                })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "ChaoxingSignFaker ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})," +
                    " buildDate: ${BuildConfig.releaseDate}," +
                    " " +
                    "Original: @aquamarine5 · Modified by rD227 · AGPL-3.0",
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = Color.Gray,
            modifier = Modifier
        )
        var isUiDevelopedMode by remember { mutableStateOf(isDevelopedMode) }
        Column(modifier = Modifier.zIndex(1f)) {
            Spacer(modifier = Modifier.height(8.dp))
            fun setIgnoreAllConsistentDeviceCodeComponents(checked: Boolean) {
                isIgnoreAllConsistentDeviceCodeComponents = checked
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                coroutineScope.launch(Dispatchers.IO) {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setPreferences(
                            it.preferences.toBuilder().setIsIgnoreAllConsistentDeviceCodeComponents(
                                isIgnoreAllConsistentDeviceCodeComponents
                            ).build()
                        ).build()
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_tablet_smartphone_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "关闭设备码提示图标",
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .clickable {
                            setIgnoreAllConsistentDeviceCodeComponents(
                                !isIgnoreAllConsistentDeviceCodeComponents
                            )
                        },
                    lineHeight = 15.sp
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 6.dp)
                )
                Switch(isIgnoreAllConsistentDeviceCodeComponents, onCheckedChange = { checked ->
                    setIgnoreAllConsistentDeviceCodeComponents(checked)
                })
            }

            Spacer(modifier = Modifier.height(2.dp))
            fun setAllowExternalProviderFetchInformation(checked: Boolean) {
                isAllowExternalProviderFetchInformation = checked
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                coroutineScope.launch(Dispatchers.IO) {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setPreferences(
                            it.preferences.toBuilder().setIsAllowExternalProviderFetchInformation(
                                isAllowExternalProviderFetchInformation
                            ).build()
                        ).build()
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_server_plus),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "允许其他应用查询学习通信息",
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .clickable {
                            setAllowExternalProviderFetchInformation(
                                !isAllowExternalProviderFetchInformation
                            )
                        },
                    lineHeight = 16.sp
                )
                IconButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isExternalProviderInformationTipsDialogVisible = true
                }) {
                    Icon(
                        painterResource(R.drawable.ic_circle_question_mark),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Gray
                    )
                }
                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 6.dp)
                )
                Switch(isAllowExternalProviderFetchInformation, onCheckedChange = { checked ->
                    setAllowExternalProviderFetchInformation(checked)
                })
            }
            if (isExternalProviderInformationTipsDialogVisible) {
                SnackbarAlertDialog(onDismissRequest = {
                    isExternalProviderInformationTipsDialogVisible = false
                }, confirmButton = {
                    Button(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        isExternalProviderInformationTipsDialogVisible = false
                    }) {
                        Text("我知道了")
                    }
                }, icon = {
                    Icon(painterResource(R.drawable.ic_triangle_alert), null)
                }, text = {
                    Column {
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.Red)) {
                                append("启用此选项会允许其他应用查询你的学习通相关信息")
                            }
                            append(
                                "，包括但不限于加入的课程信息以及签到事件信息。关闭此选项不会阻止其他应用请求跳转到签到页或课程详情页的操作。"
                            )
                        })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("你可以随时在设置页修改此设置。")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                isAllowExternalProviderFetchInformation,
                                onCheckedChange = { checked ->
                                    setAllowExternalProviderFetchInformation(checked)
                                })
                            Text(
                                "允许其他应用查询学习通信息",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                                    .clickable {
                                        setAllowExternalProviderFetchInformation(
                                            !isAllowExternalProviderFetchInformation
                                        )
                                    }
                            )
                        }
                    }
                })
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_wrench),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "启用开发模式",
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .clickable {
                            isUiDevelopedMode = !isUiDevelopedMode
                            isDevelopedMode = isUiDevelopedMode
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            coroutineScope.launch(Dispatchers.IO) {
                                context.chaoxingDataStore.updateData {
                                    it.toBuilder().setPreferences(
                                        it.preferences.toBuilder().setIsDevelopedMode(
                                            isUiDevelopedMode
                                        ).build()
                                    ).build()
                                }
                            }
                        },
                    lineHeight = 15.sp
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 6.dp)
                )
                Switch(isUiDevelopedMode, onCheckedChange = { value ->
                    isUiDevelopedMode = value
                    isDevelopedMode = value
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch(Dispatchers.IO) {
                        context.chaoxingDataStore.updateData {
                            it.toBuilder().setPreferences(
                                it.preferences.toBuilder().setIsDevelopedMode(value).build()
                            ).build()
                        }
                    }
                })
            }
        }
        @OnlyAppDevelopedMode AnimatedVisibility(
            isUiDevelopedMode,
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("总是强制签到", modifier = Modifier.weight(1f))
                    Switch(isAlwaysForceSign, onCheckedChange = { isEnabled ->
                        coroutineScope.launch {
                            val data = context.chaoxingDataStore.updateData {
                                it.toBuilder().setPreferences(
                                    it.preferences.toBuilder().setAlwaysForceSign(isEnabled)
                                ).build()
                            }
                            isAlwaysForceSign = data.preferences.alwaysForceSign
                        }
                    })
                }
                var isDataStoreDialogVisible by remember { mutableStateOf(false) }
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch(Dispatchers.IO) {
                        context.chaoxingDataStore.updateData {
                            it.toBuilder().clearLearntTooltips().build()
                        }
                    }
                }) {
                    Text("ResetAllStoredLearntTooltips")
                }
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isDataStoreDialogVisible = true
                }) {
                    Text("LoadCurrentDataStore")
                }
                if (isDataStoreDialogVisible) {
                    CurrentDataStoreDialog(onDismissRequest = {
                        isDataStoreDialogVisible = false
                    })
                }
            }
        }
    }
}
