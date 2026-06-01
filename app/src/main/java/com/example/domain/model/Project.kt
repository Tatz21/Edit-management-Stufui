package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "projects")
@JsonClass(generateAdapter = true)
data class Project(
    @PrimaryKey val projectId: String,
    val clientName: String,
    val clientPhone: String,
    val clientEmail: String,
    val projectTitle: String,
    val projectType: String,
    val description: String,
    val receivedDate: String,
    val startDate: String,
    val deadlineDate: String,
    val deliveryDate: String,
    val assignedEditor: String, // Mritunjay, Rijhu, Didi, or Unassigned
    val status: String, // New, Assigned, Editing, Preview Sent, Revision, Final Delivery, Completed, On Hold
    val priority: String, // Low, Medium, High, Urgent
    val totalAmount: Double,
    val advanceAmount: Double,
    val remainingAmount: Double, // Auto-computed: totalAmount - advanceAmount
    val paymentStatus: String, // Pending, Partially Paid, Fully Paid
    val previewSentDate: String,
    val previewVersion: String,
    val previewApproved: Boolean,
    val notes: String,
    val tags: String = "", // Comma-separated tags
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Serialized JSON Strings for Lists
    val paymentHistory: String = "[]",
    val previewHistory: String = "[]",
    val revisionHistory: String = "[]",
    val activityLogs: String = "[]",
    
    // Offline status flags
    val isSynced: Boolean = false,
    val isDeletedOffline: Boolean = false
) {
    // Companion list of predefined editors
    companion object {
        val EDITORS = listOf("Mritunjay", "Rijhu", "Didi")
        val STATUS_OPTIONS = listOf("New", "Assigned", "Editing", "Preview Sent", "Revision", "Final Delivery", "Completed", "On Hold")
        val PRIORITY_OPTIONS = listOf("Low", "Medium", "High", "Urgent")
        val PAYMENT_STATUS_OPTIONS = listOf("Pending", "Partially Paid", "Fully Paid")
    }
}

@JsonClass(generateAdapter = true)
data class PaymentHistoryEntry(
    val amount: Double,
    val date: String,
    val notes: String
)

@JsonClass(generateAdapter = true)
data class PreviewHistoryEntry(
    val version: String,
    val date: String,
    val status: String, // Approved, Not Approved
    val notes: String
)

@JsonClass(generateAdapter = true)
data class RevisionHistoryEntry(
    val version: String,
    val date: String,
    val details: String,
    val editorNotes: String
)

@JsonClass(generateAdapter = true)
data class ActivityLogEntry(
    val timestamp: Long,
    val message: String,
    val author: String
)
