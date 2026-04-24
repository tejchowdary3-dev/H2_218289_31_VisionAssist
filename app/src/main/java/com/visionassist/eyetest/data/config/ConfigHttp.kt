package com.visionassist.eyetest.data.config

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

internal object ConfigHttp {
    val DefaultClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
}
