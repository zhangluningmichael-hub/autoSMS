package com.example.autosms.repository

import com.example.autosms.database.AppDatabase
import com.example.autosms.model.SmsRule
import kotlinx.coroutines.flow.Flow

class SmsRuleRepository(private val database: AppDatabase) {
    fun getAllRules(): Flow<List<SmsRule>> {
        return database.smsRuleDao().getAllRules()
    }
    
    fun getEnabledRules(): Flow<List<SmsRule>> {
        return database.smsRuleDao().getEnabledRules()
    }
    
    suspend fun getRuleById(id: Long): SmsRule? {
        return database.smsRuleDao().getRuleById(id)
    }
    
    suspend fun insertRule(rule: SmsRule): Long {
        return database.smsRuleDao().insert(rule)
    }
    
    suspend fun updateRule(rule: SmsRule) {
        database.smsRuleDao().update(rule)
    }
    
    suspend fun deleteRule(rule: SmsRule) {
        database.smsRuleDao().delete(rule)
    }
    
    suspend fun deleteRuleById(id: Long) {
        database.smsRuleDao().deleteById(id)
    }
}