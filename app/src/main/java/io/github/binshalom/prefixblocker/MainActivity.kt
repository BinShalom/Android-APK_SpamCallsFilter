package io.github.binshalom.prefixblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val repo by lazy { BlockRepository.get(this) }
    private var roleHeld by mutableStateOf(false)
    private var contactsGranted by mutableStateOf(false)

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshRole() }

    private val contactsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            contactsGranted = granted
            repo.setAllowContacts(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                repo = repo,
                roleHeld = roleHeld,
                contactsGranted = contactsGranted,
                onRequestRole = ::requestRole,
                onToggleContacts = ::toggleContacts
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRole()
        contactsGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!contactsGranted && repo.isAllowContacts()) repo.setAllowContacts(false)
    }

    private fun refreshRole() {
        val rm = getSystemService(RoleManager::class.java)
        roleHeld = rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestRole() {
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        }
    }

    private fun toggleContacts(on: Boolean) {
        if (on && !contactsGranted) {
            contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            repo.setAllowContacts(on)
        }
    }
}
