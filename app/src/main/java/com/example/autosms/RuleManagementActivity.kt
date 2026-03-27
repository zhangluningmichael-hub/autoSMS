package com.example.autosms

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.autosms.database.AppDatabase
import com.example.autosms.model.SmsRule
import com.example.autosms.repository.SmsRuleRepository
import com.example.autosms.viewmodel.SmsRuleViewModel
import com.example.autosms.viewmodel.SmsRuleViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RuleManagementActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var fabAddRule: FloatingActionButton
    private lateinit var adapter: RuleAdapter
    private lateinit var viewModel: SmsRuleViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_management)
        
        initViews()
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.rvRules)
        tvEmpty = findViewById(R.id.tvEmpty)
        fabAddRule = findViewById(R.id.fabAddRule)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = SmsRuleRepository(database)
        val factory = SmsRuleViewModelFactory(repository)
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[SmsRuleViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        adapter = RuleAdapter { rule ->
            showRuleOptions(rule)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        viewModel.allRules.observe(this) { rules ->
            adapter.submitList(rules)
            tvEmpty.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun setupClickListeners() {
        fabAddRule.setOnClickListener {
            val intent = Intent(this, RuleEditActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun showRuleOptions(rule: SmsRule) {
        AlertDialog.Builder(this)
            .setTitle(rule.name)
            .setItems(arrayOf("编辑", "删除")) { _, which ->
                when (which) {
                    0 -> editRule(rule)
                    1 -> deleteRule(rule)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun editRule(rule: SmsRule) {
        val intent = Intent(this, RuleEditActivity::class.java)
        intent.putExtra("rule_id", rule.id)
        startActivity(intent)
    }
    
    private fun deleteRule(rule: SmsRule) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除规则 '${rule.name}' 吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteRule(rule)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class RuleAdapter(
    private val onRuleClick: (SmsRule) -> Unit
) : androidx.recyclerview.widget.ListAdapter<SmsRule, RuleAdapter.ViewHolder>(RuleDiffCallback()) {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRuleName: TextView = view.findViewById(R.id.tvRuleName)
        val tvSenderPattern: TextView = view.findViewById(R.id.tvSenderPattern)
        val tvContentKeywords: TextView = view.findViewById(R.id.tvContentKeywords)
        val tvActionType: TextView = view.findViewById(R.id.tvActionType)
        val switchRuleEnabled: android.widget.Switch = view.findViewById(R.id.switchRuleEnabled)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rule, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rule = getItem(position)
        
        holder.tvRuleName.text = rule.name
        holder.tvSenderPattern.text = "发件人: ${rule.senderPattern}"
        holder.tvContentKeywords.text = "关键词: ${rule.contentKeywords.ifBlank { "无" }}"
        
        val actionText = when (rule.actionType) {
            SmsRule.ACTION_REPLY -> "回复: ${rule.replyContent}"
            SmsRule.ACTION_FORWARD -> "转发到: ${rule.forwardNumber}"
            else -> "未知操作"
        }
        holder.tvActionType.text = actionText
        
        holder.switchRuleEnabled.isChecked = rule.enabled
        holder.switchRuleEnabled.setOnCheckedChangeListener { _, isChecked ->
            // 这里可以添加更新规则启用状态的逻辑
        }
        
        holder.btnEdit.setOnClickListener {
            onRuleClick(rule)
        }
        
        holder.btnDelete.setOnClickListener {
            onRuleClick(rule)
        }
    }
}

class RuleDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<SmsRule>() {
    override fun areItemsTheSame(oldItem: SmsRule, newItem: SmsRule): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: SmsRule, newItem: SmsRule): Boolean {
        return oldItem == newItem
    }
}