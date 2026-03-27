package com.example.autosms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.autosms.database.AppDatabase
import com.example.autosms.model.SmsRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "收到短信广播")
        
        // 检查应用是否在前台运行
        val isAppInForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
            Lifecycle.State.STARTED
        )
        
        if (!isAppInForeground) {
            Log.d(TAG, "应用在后台，启动服务处理短信")
            val serviceIntent = Intent(context, SmsProcessingService::class.java)
            serviceIntent.putExtras(intent.extras ?: Intent().extras ?: Bundle())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            return
        }
        
        // 应用在前台，直接处理
        processSms(context, intent)
    }
    
    private fun processSms(context: Context, intent: Intent) {
        val smsMessages = getSmsMessages(intent) ?: return
        
        for (sms in smsMessages) {
            val sender = sms.originatingAddress ?: continue
            val messageBody = sms.messageBody ?: continue
            
            Log.d(TAG, "收到短信 - 发件人: $sender, 内容: $messageBody")
            
            // 在后台线程处理规则匹配
            CoroutineScope(Dispatchers.IO).launch {
                processWithRules(context, sender, messageBody)
            }
        }
    }
    
    private fun getSmsMessages(intent: Intent): Array<SmsMessage>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } else {
            @Suppress("DEPRECATION")
            val pdus = intent.extras?.get("pdus") as? Array<*> ?: return null
            pdus.mapNotNull { pdu ->
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(pdu as ByteArray)
            }.toTypedArray()
        }
    }
    
    private suspend fun processWithRules(context: Context, sender: String, content: String) {
        val database = AppDatabase.getDatabase(context)
        val repository = com.example.autosms.repository.SmsRuleRepository(database)
        
        // 获取所有启用的规则
        val rules = runBlocking {
            repository.getEnabledRules().firstOrNull() ?: emptyList()
        }
        
        for (rule in rules) {
            if (rule.matches(sender, content)) {
                Log.d(TAG, "规则匹配: ${rule.name}")
                executeRuleAction(context, rule, sender, content)
            }
        }
    }
    
    private fun executeRuleAction(context: Context, rule: SmsRule, sender: String, originalContent: String) {
        when (rule.actionType) {
            SmsRule.ACTION_REPLY -> {
                rule.replyContent?.let { replyContent ->
                    sendSms(context, sender, replyContent)
                    Log.d(TAG, "已发送回复到: $sender")
                }
            }
            SmsRule.ACTION_FORWARD -> {
                rule.forwardNumber?.let { forwardNumber ->
                    val forwardContent = "转发自 $sender: $originalContent"
                    sendSms(context, forwardNumber, forwardContent)
                    Log.d(TAG, "已转发到: $forwardNumber")
                }
            }
        }
    }
    
    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "短信发送成功: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "短信发送失败", e)
        }
    }
}