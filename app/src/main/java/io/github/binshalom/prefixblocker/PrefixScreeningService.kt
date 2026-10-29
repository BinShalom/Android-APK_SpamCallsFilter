package io.github.binshalom.prefixblocker

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService

class PrefixScreeningService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        val repo = BlockRepository.get(this)
        val raw = details.handle?.schemeSpecificPart

        // Only screen incoming calls with a visible number, and only when enabled.
        if (details.callDirection != Call.Details.DIRECTION_INCOMING ||
            raw.isNullOrBlank() || !repo.isEnabled()
        ) {
            respondToCall(details, CallResponse.Builder().build())
            return
        }

        val decision = NumberMatcher.decide(
            rawNumber = raw,
            prefixes = repo.prefixSnapshot(),
            exceptions = repo.exceptionSnapshot(),
            allowContacts = repo.isAllowContacts(),
            isContact = ::isInContacts
        )

        val response = when (decision) {
            is Decision.Block -> {
                repo.logBlocked(NumberMatcher.normalize(raw), decision.matchedPrefix)
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipNotification(true)
                    .setSkipCallLog(false) // keep it visible in call history
                    .build()
            }
            is Decision.Allow -> CallResponse.Builder().build()
        }
        respondToCall(details, response)
    }

    private fun isInContacts(number: String): Boolean {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
            )
            contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null
            )?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
