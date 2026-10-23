/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.runtime.Immutable
import com.google.mlkit.vision.barcode.common.Barcode
import androidx.core.net.toUri
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkPredictable
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper

@Immutable
data class ChaoxingOtherUserSharedEntity(
    val phoneNumber: String,
    val encryptedPassword: String,
    val userName: String,
    val faceObjectIds: List<String> = emptyList(),
    val deviceCode: String? = null,
) {
    companion object {
        fun parseFromQRCode(qrcode: Barcode): ChaoxingOtherUserSharedEntity {
            return runCatching {
                val raw = qrcode.rawValue ?: qrcode.url?.url
                checkPredictable(!raw.isNullOrBlank()) { "二维码不是一个有效的链接" }
                val url = raw.toUri()
                checkPredictable(url.scheme in listOf("rd227cxsignfaker", "cxsignfaker") && url.host == "import" ||
                    url.scheme in listOf("http", "https") && url.host == "cdn.aquamarine5.fun") {
                    "二维码不是一个有效的用户分享链接"
                }
                val phoneNumber = url.getQueryParameter("phone")!!
                val password = url.getQueryParameter("pwd")!!
                val userName = url.getQueryParameter("name")!!
                val faceObjectIds = url.getQueryParameter("face")
                    ?.split(',')
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    .orEmpty()
                val deviceCode = url.getQueryParameter("dc")?.takeIf { it.isNotEmpty() }
                ChaoxingOtherUserSharedEntity(
                    phoneNumber,
                    password,
                    userName,
                    faceObjectIds,
                    deviceCode,
                )
            }.getOrElse {
                throw ChaoxingOtherUserHelper.NotAvailableQRCodeException("此二维码不能作用于添加用户")
            }
        }
    }
}
