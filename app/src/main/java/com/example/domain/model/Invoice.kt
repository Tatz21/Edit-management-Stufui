package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "invoices")
@JsonClass(generateAdapter = true)
data class Invoice(
    @PrimaryKey val invoiceId: String,
    val clientId: String,
    val clientName: String,
    val projectId: String?, // Optional link to specific project
    val projectName: String?, // Optional project title
    val invoiceNumber: String, // e.g., INV-2026-001
    val amount: Double,
    val status: String, // Paid, Pending, Overdue
    val issueDate: String, // yyyy-MM-dd
    val dueDate: String, // yyyy-MM-dd
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val STATUS_OPTIONS = listOf("Pending", "Paid", "Overdue")
    }
}
