package io.github.binshalom.prefixblocker

sealed class Decision {
    data class Allow(val reason: String) : Decision()
    data class Block(val matchedPrefix: String) : Decision()
}

object NumberMatcher {

    /** Digits only, with a leading US/Canada "1" country code removed from 11-digit numbers. */
    fun normalize(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length == 11 && digits.startsWith("1")) digits.substring(1) else digits
    }

    /**
     * Order of precedence:
     *  1. Exception list (exact number)  -> allow
     *  2. Known contact (if enabled)     -> allow
     *  3. Matching blocked prefix        -> block
     *  4. Otherwise                      -> allow
     */
    fun decide(
        rawNumber: String,
        prefixes: List<String>,
        exceptions: List<String>,
        allowContacts: Boolean,
        isContact: (String) -> Boolean
    ): Decision {
        val number = normalize(rawNumber)

        val matched = prefixes.firstOrNull { number.startsWith(normalizePrefix(it)) }
            ?: return Decision.Allow("no matching prefix")

        if (number in exceptions) return Decision.Allow("exception list")
        if (allowContacts && isContact(rawNumber)) return Decision.Allow("contact")
        return Decision.Block(matched)
    }

    // A prefix typed as "1800" should behave like "800".
    private fun normalizePrefix(p: String) =
        if (p.length > 1 && p.startsWith("1")) p.substring(1) else p
}
