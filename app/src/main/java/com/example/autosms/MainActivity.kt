package com.example.autosms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.autosms.database.AppDatabase
import com.example.autosms.repository.SmsRuleRepository

class MainActivity : AppCompatActivity() {
    
    private lateinit var tvServiceStatus: TextView
    private lateinit var btnToggleService: Button
    private lateinit var btnRequestPermission: Button
    private lateinit var btnManageRules: Button
    
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
    }
    
    private val PERMISSION_REQUEST_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        updateServiceStatus()
    }
    
    private fun initViews() {
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnRequestPermission = findViewById(R.id.btnRequestPermission)
        btnManageRules = findViewById(R.id.btnManageRules)
    }
    
    private fun setupClickListeners() {
        btnRequestPermission.setOnClickListener {
            requestPermissions()
        }
        
        btnToggleService.setOnClickListener {
            toggleService()
        }
        
        btnManageRules.setOnClickListener {
            val intent = Intent(this, RuleManagementActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun requestPermissions() {
        if (hasAllPermissions()) {
            Toast.makeText(this, "所有权限已获取", Toast.LENGTH_SHORT).show()
            updateServiceStatus()
            return
        }
        
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }
    
    private fun hasAllPermissions(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun toggleService() {
        if (!hasAllPermissions()) {
            Toast.makeText(this, "请先获取所有权限", Toast.LENGTH_SHORT).show()
            requestPermissions()
            return
        }
        
        val isServiceEnabled = PreferenceHelper.isServiceEnabled(this)
        PreferenceHelper.setServiceEnabled(this, !isServiceEnabled)
        updateServiceStatus()
        
        val status = if (!isServiceEnabled) "已启用" else "已禁用"
        Toast.makeText(this, "服务$status", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateServiceStatus() {
        val hasPermissions = hasAllPermissions()
        val isEnabled = PreferenceHelper.isServiceEnabled(this)
        
        val statusText = when {
            !hasPermissions -> "需要权限"
            isEnabled -> "已启用"
            else -> "已禁用"
        }
        
        tvServiceStatus.text = getString(R.string.service_status, statusText)
        btnToggleService.text = if (isEnabled) getString(R.string.disable_service) else getString(R.string.enable_service)
        btnToggleService.isEnabled = hasPermissions
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allGranted) {
                Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "部分权限被拒绝，功能可能受限", Toast.LENGTH_SHORT).show()
            }
            
            updateServiceStatus()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }
}

object PreferenceHelper {
    private const val PREF_NAME = "autosms_prefs"
    private const val KEY_SERVICE_ENABLED = "service_enabled"
    
    fun isServiceEnabled(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SERVICE_ENABLED, true)
    }
    
    fun setServiceEnabled(context: android.content.Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }
}