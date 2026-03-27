package com.example.autosms

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.autosms.database.AppDatabase
import com.example.autosms.model.SmsRule
import com.example.autosms.repository.SmsRuleRepository
import com.example.autosms.viewmodel.SmsRuleViewModel
import com.example.autosms.viewmodel.SmsRuleViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RuleEditActivity : AppCompatActivity() {
    
    private lateinit var etRuleName: TextInputEditText
    private lateinit var etSenderPattern: TextInputEditText
    private lateinit var etContentKeywords: TextInputEditText
    private lateinit var rgActionType: RadioGroup
    private lateinit var rbReply: RadioButton
    private lateinit var rbForward: RadioButton
    private lateinit var etReplyContent: TextInputEditText
    private lateinit var tilReplyContent: TextInputLayout
    private lateinit var etForwardNumber: TextInputEditText
    private lateinit var tilForwardNumber: TextInputLayout
    private lateinit var switchEnabled: Switch
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    
    private lateinit var viewModel: SmsRuleViewModel
    private var ruleId: Long = -1
    private var isEditMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_edit)
        
        initViews()
        setupViewModel()
        setupClickListeners()
        loadRuleData()
    }
    
    private fun initViews() {
        etRuleName = findViewById(R.id.etRuleName)
        etSenderPattern = findViewById(R.id.etSenderPattern)
        etContentKeywords = findViewById(R.id.etContentKeywords)
        rgActionType = findViewById(R.id.rgActionType)
        rbReply = findViewById(R.id.rbReply)
        rbForward = findViewById(R.id.rbForward)
        etReplyContent = findViewById(R.id.etReplyContent)
        tilReplyContent = findViewById(R.id.tilReplyContent)
        etForwardNumber = findViewById(R.id.etForwardNumber)
        tilForwardNumber = findViewById(R.id.tilForwardNumber)
        switchEnabled = findViewById(R.id.switchEnabled)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "编辑规则"
    }
    
    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = SmsRuleRepository(database)
        val factory = SmsRuleViewModelFactory(repository)
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[SmsRuleViewModel::class.java]
    }
    
    private fun setupClickListeners() {
        rgActionType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbReply -> {
                    tilReplyContent.visibility = android.view.View.VISIBLE
                    tilForwardNumber.visibility = android.view.View.GONE
                }
                R.id.rbForward -> {
                    tilReplyContent.visibility = android.view.View.GONE
                    tilForwardNumber.visibility = android.view.View.VISIBLE
                }
            }
        }
        
        btnSave.setOnClickListener {
            saveRule()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
        
        // 默认选择回复
        rbReply.isChecked = true
    }
    
    private fun loadRuleData() {
        ruleId = intent.getLongExtra("rule_id", -1)
        isEditMode = ruleId != -1L
        
        if (isEditMode) {
            CoroutineScope(Dispatchers.IO).launch {
                val rule = viewModel.getRuleById(ruleId)
                rule?.let { populateForm(it) }
            }
        }
    }
    
    private fun populateForm(rule: SmsRule) {
        runOnUiThread {
            etRuleName.setText(rule.name)
            etSenderPattern.setText(rule.senderPattern)
            etContentKeywords.setText(rule.contentKeywords)
            switchEnabled.isChecked = rule.enabled
            
            when (rule.actionType) {
                SmsRule.ACTION_REPLY -> {
                    rbReply.isChecked = true
                    etReplyContent.setText(rule.replyContent)
                }
                SmsRule.ACTION_FORWARD -> {
                    rbForward.isChecked = true
                    etForwardNumber.setText(rule.forwardNumber)
                }
            }
        }
    }
    
    private fun saveRule() {
        val name = etRuleName.text.toString().trim()
        val senderPattern = etSenderPattern.text.toString().trim()
        val contentKeywords = etContentKeywords.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入规则名称", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (senderPattern.isEmpty()) {
            Toast.makeText(this, "请输入发件人匹配模式", Toast.LENGTH_SHORT).show()
            return
        }
        
        val actionType = when (rgActionType.checkedRadioButtonId) {
            R.id.rbReply -> SmsRule.ACTION_REPLY
            R.id.rbForward -> SmsRule.ACTION_FORWARD
            else -> SmsRule.ACTION_REPLY
        }
        
        val replyContent = if (actionType == SmsRule.ACTION_REPLY) {
            etReplyContent.text.toString().trim().takeIf { it.isNotEmpty() }
        } else null
        
        val forwardNumber = if (actionType == SmsRule.ACTION_FORWARD) {
            etForwardNumber.text.toString().trim().takeIf { it.isNotEmpty() }
        } else null
        
        if (actionType == SmsRule.ACTION_REPLY && replyContent.isNullOrEmpty()) {
            Toast.makeText(this, "请输入回复内容", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (actionType == SmsRule.ACTION_FORWARD && forwardNumber.isNullOrEmpty()) {
            Toast.makeText(this, "请输入转发号码", Toast.LENGTH_SHORT).show()
            return
        }
        
        val rule = if (isEditMode) {
            SmsRule(
                id = ruleId,
                name = name,
                senderPattern = senderPattern,
                contentKeywords = contentKeywords,
                actionType = actionType,
                replyContent = replyContent,
                forwardNumber = forwardNumber,
                enabled = switchEnabled.isChecked
            )
        } else {
            SmsRule(
                name = name,
                senderPattern = senderPattern,
                contentKeywords = contentKeywords,
                actionType = actionType,
                replyContent = replyContent,
                forwardNumber = forwardNumber,
                enabled = switchEnabled.isChecked
            )
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            if (isEditMode) {
                viewModel.updateRule(rule)
            } else {
                viewModel.insertRule(rule)
            }
            
            runOnUiThread {
                Toast.makeText(this@RuleEditActivity, "规则保存成功", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}