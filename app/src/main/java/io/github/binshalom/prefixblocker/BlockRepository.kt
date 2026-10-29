package io.github.binshalom.prefixblocker

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BlockedCall(val time: Long, val number: String, val prefix: String)

/**
 * Simple SharedPreferences-backed store. SharedPreferences is synchronous,
 * which is what we want inside the screening service where we must answer fast.
 */
class BlockRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("block_prefs", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(K_ENABLED, true))
    private val _allowContacts = MutableStateFlow(prefs.getBoolean(K_CONTACTS, false))
    private val _prefixes = MutableStateFlow(loadSet(K_PREFIXES))
    private val _exceptions = MutableStateFlow(loadSet(K_EXCEPTIONS))
    private val _log = MutableStateFlow(loadLog())

    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    val allowContacts: StateFlow<Boolean> = _allowContacts.asStateFlow()
    val log: StateFlow<List<BlockedCall>> = _log.asStateFlow()

    // Exposed as state flows of lists for Compose
    private val prefixList = MutableStateFlow(_prefixes.value)
    private val exceptionList = MutableStateFlow(_exceptions.value)
    val prefixFlow: StateFlow<List<String>> = prefixList.asStateFlow()
    val exceptionFlow: StateFlow<List<String>> = exceptionList.asStateFlow()

    // ---- snapshot reads for the service ----
    fun isEnabled() = _enabled.value
    fun isAllowContacts() = _allowContacts.value
    fun prefixSnapshot(): List<String> = prefixList.value
    fun exceptionSnapshot(): List<String> = exceptionList.value

    // ---- mutations ----
    fun setEnabled(v: Boolean) {
        prefs.edit().putBoolean(K_ENABLED, v).apply(); _enabled.value = v
    }

    fun setAllowContacts(v: Boolean) {
        prefs.edit().putBoolean(K_CONTACTS, v).apply(); _allowContacts.value = v
    }

    fun addPrefix(p: String) {
        val clean = p.filter { it.isDigit() }
        if (clean.isEmpty()) return
        saveSet(K_PREFIXES, prefixList, (prefixList.value + clean).distinct().sorted())
    }

    fun removePrefix(p: String) =
        saveSet(K_PREFIXES, prefixList, prefixList.value - p)

    fun addException(n: String) {
        val clean = NumberMatcher.normalize(n)
        if (clean.length < 7) return
        saveSet(K_EXCEPTIONS, exceptionList, (exceptionList.value + clean).distinct().sorted())
    }

    fun removeException(n: String) =
        saveSet(K_EXCEPTIONS, exceptionList, exceptionList.value - n)

    fun logBlocked(number: String, prefix: String) {
        val updated = (listOf(BlockedCall(System.currentTimeMillis(), number, prefix)) + _log.value)
            .take(MAX_LOG)
        prefs.edit().putString(K_LOG, updated.joinToString("\n") { "${it.time}|${it.number}|${it.prefix}" }).apply()
        _log.value = updated
    }

    fun clearLog() {
        prefs.edit().remove(K_LOG).apply(); _log.value = emptyList()
    }

    // ---- helpers ----
    private fun saveSet(key: String, flow: MutableStateFlow<List<String>>, value: List<String>) {
        prefs.edit().putStringSet(key, value.toSet()).apply()
        flow.value = value.sorted()
    }

    private fun loadSet(key: String): List<String> =
        (prefs.getStringSet(key, emptySet()) ?: emptySet()).toList().sorted()

    private fun loadLog(): List<BlockedCall> =
        (prefs.getString(K_LOG, "") ?: "").lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 3) parts[0].toLongOrNull()?.let { BlockedCall(it, parts[1], parts[2]) } else null
        }

    companion object {
        private const val K_ENABLED = "enabled"
        private const val K_CONTACTS = "allow_contacts"
        private const val K_PREFIXES = "prefixes"
        private const val K_EXCEPTIONS = "exceptions"
        private const val K_LOG = "log"
        private const val MAX_LOG = 50

        @Volatile private var instance: BlockRepository? = null
        fun get(context: Context): BlockRepository =
            instance ?: synchronized(this) {
                instance ?: BlockRepository(context).also { instance = it }
            }
    }
}
