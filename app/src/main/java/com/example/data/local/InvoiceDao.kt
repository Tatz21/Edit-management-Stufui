package com.example.data.local

import androidx.room.*
import com.example.domain.model.Invoice
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoicesFlow(): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice)

    @Query("DELETE FROM invoices WHERE invoiceId = :id")
    suspend fun deleteInvoice(id: String)

    @Query("SELECT * FROM invoices WHERE invoiceId = :id")
    suspend fun getInvoiceById(id: String): Invoice?

    @Query("SELECT * FROM invoices WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getInvoicesForClientFlow(clientId: String): Flow<List<Invoice>>
}
