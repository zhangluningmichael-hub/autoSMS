package com.example.autosms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.autosms.model.SmsRule
import com.example.autosms.repository.SmsRuleRepository
import kotlinx.coroutines.launch

class SmsRuleViewModel(private val repository: SmsRuleRepository) : ViewModel() {
    val allRules = repository.getAllRules().asLiveData()
    val enabledRules = repository.getEnabledRules().asLiveData()
    
    fun getRuleById(id: Long) = viewModelScope.launch {
        repository.getRuleById(id)
    }
    
    fun insertRule(rule: SmsRule) = viewModelScope.launch {
        repository.insertRule(rule)
    }
    
    fun updateRule(rule: SmsRule) = viewModelScope.launch {
        repository.updateRule(rule)
    }
    
    fun deleteRule(rule: SmsRule) = viewModelScope.launch {
        repository.deleteRule(rule)
    }
    
    fun deleteRuleById(id: Long) = viewModelScope.launch {
        repository.deleteRuleById(id)
    }
}

class SmsRuleViewModelFactory(private val repository: SmsRuleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmsRuleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SmsRuleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}