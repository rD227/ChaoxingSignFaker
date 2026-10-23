package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import java.security.MessageDigest

internal fun md5(value: String): String =
    MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8)).toHexString()
