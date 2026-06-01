package com.example.util

import com.example.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonUtil {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val paymentListType = Types.newParameterizedType(List::class.java, PaymentHistoryEntry::class.java)
    private val previewListType = Types.newParameterizedType(List::class.java, PreviewHistoryEntry::class.java)
    private val revisionListType = Types.newParameterizedType(List::class.java, RevisionHistoryEntry::class.java)
    private val activityListType = Types.newParameterizedType(List::class.java, ActivityLogEntry::class.java)

    private val paymentAdapter = moshi.adapter<List<PaymentHistoryEntry>>(paymentListType)
    private val previewAdapter = moshi.adapter<List<PreviewHistoryEntry>>(previewListType)
    private val revisionAdapter = moshi.adapter<List<RevisionHistoryEntry>>(revisionListType)
    private val activityAdapter = moshi.adapter<List<ActivityLogEntry>>(activityListType)

    fun toPaymentHistoryJson(list: List<PaymentHistoryEntry>): String = paymentAdapter.toJson(list)
    fun fromPaymentHistoryJson(json: String?): List<PaymentHistoryEntry> {
        if (json.isNullOrEmpty()) return emptyList()
        return try { paymentAdapter.fromJson(json) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun toPreviewHistoryJson(list: List<PreviewHistoryEntry>): String = previewAdapter.toJson(list)
    fun fromPreviewHistoryJson(json: String?): List<PreviewHistoryEntry> {
        if (json.isNullOrEmpty()) return emptyList()
        return try { previewAdapter.fromJson(json) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun toRevisionHistoryJson(list: List<RevisionHistoryEntry>): String = revisionAdapter.toJson(list)
    fun fromRevisionHistoryJson(json: String?): List<RevisionHistoryEntry> {
        if (json.isNullOrEmpty()) return emptyList()
        return try { revisionAdapter.fromJson(json) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun toActivityHistoryJson(list: List<ActivityLogEntry>): String = activityAdapter.toJson(list)
    fun fromActivityHistoryJson(json: String?): List<ActivityLogEntry> {
        if (json.isNullOrEmpty()) return emptyList()
        return try { activityAdapter.fromJson(json) ?: emptyList() } catch (e: Exception) { emptyList() }
    }
}
