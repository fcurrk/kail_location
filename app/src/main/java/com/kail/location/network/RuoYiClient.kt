package com.kail.location.network

import com.kail.location.BuildConfig
import com.kail.location.utils.KailLog
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RuoYiClient {

    private const val TAG = "RuoYiClient"
    private const val JSON_TYPE = "application/json"

    // ========== 功能总开关：false 表示禁用所有网络请求 ==========
    private const val FEATURE_ENABLED = false

    var baseUrl: String = BuildConfig.APP_API_URL

    private val trustAllCertificates = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .sslSocketFactory(
            SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustAllCertificates), java.security.SecureRandom())
            }.socketFactory,
            trustAllCertificates
        )
        .hostnameVerifier { _, _ -> true }
        .build()

    data class AuthResult(
        val token: String,
        val email: String,
        val id: String,
        val verified: Boolean
    )

    private fun Request.Builder.withTenant(): Request.Builder {
        return this.header("tenant-id", "1")
    }

    private fun Request.Builder.withAuth(token: String): Request.Builder {
        return this.header("Authorization", "Bearer $token")
    }

    // ---------- 模拟次数检查 ----------
    fun checkSimulation(token: String): Result<Int> {
        if (!FEATURE_ENABLED) {
            KailLog.w(null, TAG, "功能已关闭，checkSimulation 返回默认值 0")
            return Result.success(0)
        }
        return runCatching {
            val url = "$baseUrl/member/simulation/check"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Content-Type", JSON_TYPE)
                .withAuth(token)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")
            val root = JSONObject(body)
            val code = root.optInt("code", -1)
            if (code != 0) {
                throw Exception(root.optString("msg", "获取剩余次数失败"))
            }
            val remaining = root.getJSONObject("data").optInt("remainingToday", 0)
            KailLog.i(null, TAG, "checkSimulation: http=${response.code} code=$code remainingToday=$remaining")
            remaining
        }.onFailure { KailLog.w(null, TAG, "checkSimulation failed: ${it.message}") }
    }

    // ---------- 消耗模拟次数 ----------
    fun useSimulation(token: String): Result<Unit> {
        if (!FEATURE_ENABLED) {
            KailLog.w(null, TAG, "功能已关闭，useSimulation 返回成功")
            return Result.success(Unit)
        }
        return runCatching {
            val url = "$baseUrl/member/simulation/use"
            val request = Request.Builder()
                .url(url)
                .post("{}".toRequestBody(JSON_TYPE.toMediaType()))
                .header("Content-Type", JSON_TYPE)
                .withAuth(token)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            val root = JSONObject(body)
            val code = root.optInt("code", -1)
            KailLog.i(null, TAG, "useSimulation: http=${response.code} code=$code msg=${root.optString("msg", "")}")
            if (code != 0) {
                throw Exception(root.optString("msg", "模拟次数已用完"))
            }
        }.onFailure { KailLog.w(null, TAG, "useSimulation failed: ${it.message}") }
    }

    // ---------- 发送邮箱验证码 ----------
    fun sendMailCode(mail: String, scene: Int): Result<Unit> {
        if (!FEATURE_ENABLED) {
            KailLog.w(null, TAG, "功能已关闭，sendMailCode 返回成功")
            return Result.success(Unit)
        }
        return runCatching {
            val url = "$baseUrl/member/auth/send-mail-code"
            val json = JSONObject().apply {
                put("mail", mail)
                put("scene", scene)
            }

            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(JSON_TYPE.toMediaType()))
                .header("Content-Type", JSON_TYPE)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            val root = JSONObject(body)
            val code = root.optInt("code", -1)
            KailLog.i(null, TAG, "sendMailCode(scene=$scene): http=${response.code} code=$code")
            if (code != 0) {
                throw Exception(root.optString("msg", "发送验证码失败"))
            }
        }.onFailure { KailLog.w(null, TAG, "sendMailCode failed: ${it.message}") }
    }

    // ---------- 邮箱登录 ----------
    fun loginByMail(mail: String, code: String): Result<AuthResult> {
        if (!FEATURE_ENABLED) {
            KailLog.w(null, TAG, "功能已关闭，loginByMail 返回模拟 AuthResult")
            return Result.success(
                AuthResult(
                    token = "fake-token-${System.currentTimeMillis()}",
                    email = mail,
                    id = "0",
                    verified = true
                )
            )
        }
        return runCatching {
            val url = "$baseUrl/member/auth/mail-login"
            val json = JSONObject().apply {
                put("mail", mail)
                put("code", code)
            }

            val request = Request.Builder()
                .url(url)
                .post(json.toString().toRequestBody(JSON_TYPE.toMediaType()))
                .header("Content-Type", JSON_TYPE)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            val root = JSONObject(body)
            val respCode = root.optInt("code", -1)
            KailLog.i(null, TAG, "loginByMail: http=${response.code} code=$respCode")
            if (respCode != 0) {
                throw Exception(root.optString("msg", "登录失败"))
            }

            val data = root.getJSONObject("data")
            AuthResult(
                token = data.getString("accessToken"),
                email = mail,
                id = data.optString("userId", ""),
                verified = true
            )
        }
    }

    // ---------- 订阅状态 ----------
    data class SubscriptionStatus(
        val active: Boolean,
        val planName: String,
        val expiresAt: String,
        val daysRemaining: Int
    )

    data class SubscriptionPlan(
        val id: Long,
        val name: String,
        val description: String,
        val price: Int,
        val currency: String,
        val billingInterval: String,
        val billingIntervalCount: Int,
        val trialDays: Int
    )

    suspend fun getPlans(token: String): Result<List<SubscriptionPlan>> {
        if (!FEATURE_ENABLED) {
            KailLog.w(null, TAG, "功能已关闭，getPlans 返回空列表")
            return Result.success(emptyList())
        }
        return runCatching {
            val url = "$baseUrl/member/subscription-plan/list"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Content-Type", JSON_TYPE)
                .withAuth(token)
                .withTenant()
                .build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")
            val root = JSONObject(body)
            val respCode = root.optInt("code", -1)
            if (respCode != 0) {
                throw Exception(root.optString("msg", "获取套餐列表失败"))
            }
            val arr = root.getJSONArray("data")
            val plans = mutableListOf<SubscriptionPlan>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                plans.add(SubscriptionPlan(
                    id = item.getLong("id"),
                    name = item.getString("name"),
                    description = item.optString("description", ""),
                    price = item.getInt("price"),
                    currency = item.optString("currency", "CNY"),
                    billingInterval = item.optString("billingInterval", "month"),
                    billingIntervalCount = item.optInt("billingIntervalCount", 1),
                    trialDays = item.optInt("trialDays", 0)
                ))
            }
            KailLog.i(null, TAG, "getPlans: http=${response.code} code=$respCode count=${plans.size}")
            plans
        }.onFailure { KailLog.w(null, TAG, "getPlans failed: ${it.message}") }
    }

    data class NoticeInfo(
        val id: Long,
        val title: String,
        val type: Int,
        val content: String,
        val createTime: String
    )

    suspend fun getNoticeList(): Result<List<NoticeInfo>> {
        if (!FEATURE_ENABLED) {
            KailLog.w(null, TAG, "功能已关闭，getNoticeList 返回空列表")
            return Result.success(emptyList())
        }
        return runCatching {
            val url = "$baseUrl/system/notice/list"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Content-Type", JSON_TYPE)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")
            val root = JSONObject(body)
            val code = root.optInt("code", -1)
            if (code != 0) {
                throw Exception(root.optString("msg", "获取公告失败"))
            }
            val arr = root.optJSONArray("data") ?: return@runCatching emptyList()
            val list = mutableListOf<NoticeInfo>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                list.add(NoticeInfo(
                    id = item.getLong("id"),
                    title = item.optString("title", ""),
                    type = item.optInt("type", 0),
                    content = item.optString("content", ""),
                    createTime = item.optString("createTime", "")
                ))
            }
            list
        }.onFailure { KailLog.w(null, TAG, "getNoticeList failed: ${it.message}") }
    }

    suspend fun getSubscriptionStatus(token: String): Result<SubscriptionStatus> {
        if (!FEATURE_ENABLED) {
            KailLog.w(null, TAG, "功能已关闭，getSubscriptionStatus 返回非活跃状态")
            return Result.success(SubscriptionStatus(false, "", "", 0))
        }
        return runCatching {
            val url = "$baseUrl/member/subscription/status"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Content-Type", JSON_TYPE)
                .withAuth(token)
                .withTenant()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            val root = JSONObject(body)
            val respCode = root.optInt("code", -1)
            if (respCode != 0) {
                throw Exception(root.optString("msg", "获取订阅状态失败"))
            }

            val data = root.getJSONObject("data")
            val status = SubscriptionStatus(
                active = data.optBoolean("active", false),
                planName = data.optString("planName", ""),
                expiresAt = data.optString("expiresAt", ""),
                daysRemaining = data.optInt("daysRemaining", 0)
            )
            KailLog.i(null, TAG, "getSubscriptionStatus: http=${response.code} active=${status.active} daysRemaining=${status.daysRemaining}")
            status
        }.onFailure { KailLog.w(null, TAG, "getSubscriptionStatus failed: ${it.message}") }
    }

}