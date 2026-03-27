package com.example.autosms.database.dao

import androidx.room.*
import com.example.autosms.model.SmsRule
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsRuleDao {
    @Query("SELECT * FROM sms_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<SmsRule>>
    
    @Query("SELECT * FROM sms_rules WHERE enabled = 1")
    fun getEnabledRules(): Flow<List<SmsRule>>
    
    @Query("SELECT * FROM sms_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): SmsRule?
    
    @Insert
    suspend fun insert(rule: SmsRule): Long
    
    @Update
    suspend fun update(rule: SmsRule)
    
    @Delete
    suspend fun delete(rule: SmsRule)
    
    @Query("DELETE FROM sms_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}