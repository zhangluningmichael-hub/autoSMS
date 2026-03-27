package com.example.autosms.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_rules")
data class SmsRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val senderPattern: String, // 发件人匹配模式，支持通配符*，如138*, 10086, +86*
    val contentKeywords: String, // 内容关键词，逗号分隔
    val actionType: Int, // 0=回复，1=转发
    val replyContent: String? = null, // 回复内容（actionType=0时使用）
    val forwardNumber: String? = null, // 转发号码（actionType=1时使用）
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ACTION_REPLY = 0
        const val ACTION_FORWARD = 1
    }
    
    fun matches(sender: String, content: String): Boolean {
        // 检查发件人匹配
        val senderMatches = when {
            senderPattern.contains("*") -> {
                val pattern = senderPattern.replace("*", ".*")
                sender.matches(Regex(pattern))
            }
            else -> sender == senderPattern
        }
        
        if (!senderMatches) return false
        
        // 检查内容关键词
        if (contentKeywords.isBlank()) return true
        
        val keywords = contentKeywords.split(",").map { it.trim() }
        return keywords.any { keyword ->
            content.contains(keyword, ignoreCase = true)
        }
    }
}