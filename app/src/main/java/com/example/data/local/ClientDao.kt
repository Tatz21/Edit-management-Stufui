package com.example.data.local

import androidx.room.*
import com.example.domain.model.Client
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY createdAt DESC")
    fun getAllClientsFlow(): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Query("DELETE FROM clients WHERE clientId = :id")
    suspend fun deleteClient(id: String)

    @Query("SELECT * FROM clients WHERE clientId = :id")
    suspend fun getClientById(id: String): Client?
}
