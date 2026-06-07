package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "clients")
@JsonClass(generateAdapter = true)
data class Client(
    @PrimaryKey val clientId: String,
    val name: String,
    val email: String,
    val phone: String,
    val notes: String = "",
    val paymentStatus: String = "Pending", // Pending, Partially Paid, Fully Paid
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val PAYMENT_STATUS_OPTIONS = listOf("Pending", "Partially Paid", "Fully Paid")
    }
}
