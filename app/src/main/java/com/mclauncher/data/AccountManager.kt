package com.mclauncher.data

import com.mclauncher.MCApplication
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 离线账户系统 —— 无需 Mojang 账号
 * 格式: 本地皮肤 + 名字 + 离线 UUID
 */
object AccountManager {
    
    private val accountsFile by lazy {
        File(MCApplication.gameDir, "accounts.json").apply {
            if (!exists()) writeText("[]")
        }
    }
    
    private val _accounts = mutableListOf<MCAccount>()
    
    data class MCAccount(
        val name: String,
        val uuid: String,
        val skinUrl: String = "",
        val isActive: Boolean = true
    ) {
        val offlineAccessToken: String get() = uuid.replace("-", "")
    }
    
    init { load() }
    
    fun load() {
        try {
            _accounts.clear()
            val arr = JSONArray(accountsFile.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                _accounts += MCAccount(
                    name = o.getString("name"),
                    uuid = o.getString("uuid"),
                    skinUrl = o.optString("skinUrl"),
                    isActive = o.optBoolean("isActive", true)
                )
            }
        } catch (e: Exception) { _accounts.clear() }
    }
    
    fun save() {
        val arr = JSONArray()
        for (a in _accounts) {
            arr.put(JSONObject().apply {
                put("name", a.name)
                put("uuid", a.uuid)
                put("skinUrl", a.skinUrl)
                put("isActive", a.isActive)
            })
        }
        accountsFile.writeText(arr.toString(2))
    }
    
    fun list(): List<MCAccount> = _accounts.toList()
    
    fun active(): MCAccount? = _accounts.firstOrNull { it.isActive }
    
    /** 添加离线账户 */
    fun addOffline(name: String): MCAccount {
        val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray()).toString()
        val account = MCAccount(name = name, uuid = uuid)
        _accounts.forEach { _accounts[_accounts.indexOf(it)] = it.copy(isActive = false) }
        _accounts += account
        save()
        return account
    }
    
    fun setActive(name: String) {
        _accounts.forEachIndexed { i, a ->
            _accounts[i] = a.copy(isActive = (a.name == name))
        }
        save()
    }
    
    fun remove(name: String) {
        _accounts.removeAll { it.name == name }
        if (_accounts.none { it.isActive } && _accounts.isNotEmpty()) {
            _accounts[0] = _accounts[0].copy(isActive = true)
        }
        save()
    }
}
