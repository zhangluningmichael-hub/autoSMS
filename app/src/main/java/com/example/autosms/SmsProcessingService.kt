package com.example.autosms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.autosms.database.AppDatabase
import com.example.autosms.model.SmsRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SmsProcessingService : Service() {
    companion object {
        private const val TAG = "SmsProcessingService"
        private const val NOTIFICATION_CHANNEL_ID = "sms_processing_channel"
        private const val NOTIFICATION_ID = 1001
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服务启动")
        
        // 创建前台通知
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // 处理短信
        intent?.let { processSms(it) }
        
        // 处理完成后停止服务
        stopSelf()
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun processSms(intent: Intent) {
        val smsMessages = getSmsMessages(intent) ?: return
        
        for (sms in smsMessages) {
            val sender = sms.originatingAddress ?: continue
            val messageBody = sms.messageBody ?: continue
            
            Log.d(TAG, "处理短信 - 发件人: $sender, 内容: $messageBody")
            
            // 在后台线程处理规则匹配
            CoroutineScope(Dispatchers.IO).launch {
                processWithRules(sender, messageBody)
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
    
    private suspend fun processWithRules(sender: String, content: String) {
        val database = AppDatabase.getDatabase(this)
        val repository = com.example.autosms.repository.SmsRuleRepository(database)
        
        // 获取所有启用的规则
        val rules = runBlocking {
            repository.getEnabledRules().firstOrNull() ?: emptyList()
        }
        
        for (rule in rules) {
            if (rule.matches(sender, content)) {
                Log.d(TAG, "规则匹配: ${rule.name}")
                executeRuleAction(rule, sender, content)
            }
        }
    }
    
    private fun executeRuleAction(rule: SmsRule, sender: String, originalContent: String) {
        when (rule.actionType) {
            SmsRule.ACTION_REPLY -> {
                rule.replyContent?.let { replyContent ->
                    sendSms(sender, replyContent)
                    Log.d(TAG, "已发送回复到: $sender")
                }
            }
            SmsRule.ACTION_FORWARD -> {
                rule.forwardNumber?.let { forwardNumber ->
                    val forwardContent = "转发自 $sender: $originalContent"
                    sendSms(forwardNumber, forwardContent)
                    Log.d(TAG, "已转发到: $forwardNumber")
                }
            }
        }
    }
    
    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(android.telephony.SmsManager::class.java)
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
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "短信处理服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "处理收到的短信并执行自动回复或转发"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AutoSMS")
            .setContentText("正在处理短信...")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}