package com.example.data.helper

object ChatSanitizer {
    private val BAD_WORDS = setOf(
        "anjing", "babi", "bangsat", "tolol", "goblok", "kontol", "memek", "asuh", "bajingan", "perek", "brengsek", "kampret"
    )

    // Match typical emails
    private val EMAIL_PATTERN = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}".toRegex()

    // Match phone numbers - 8 or more digits with possible +, dashes, spaces
    private val PHONE_PATTERN = "(\\+?\\d[\\d-\\s]{6,}\\d)".toRegex()

    fun sanitize(text: String): String {
        var processed = text

        // 1. Redact Emails
        processed = EMAIL_PATTERN.replace(processed) { "[SENSITIF: EMAIL TERPROTEKSI]" }

        // 2. Redact Phone Numbers
        processed = PHONE_PATTERN.replace(processed) { matchResult ->
            val digitsOnly = matchResult.value.replace(Regex("[^0-9]"), "")
            if (digitsOnly.length >= 8) {
                "[SENSITIF: NO TELEPON TERPROTEKSI]"
            } else {
                matchResult.value
            }
        }

        // 3. Filter Swear/Bad Words
        for (badWord in BAD_WORDS) {
            // Case-insensitive replacement of bad words
            val regex = "(?i)\\b$badWord\\b".toRegex()
            processed = regex.replace(processed) {
                "*".repeat(badWord.length)
            }
            
            // Substring case in case they group characters
            val regexSub = "(?i)$badWord".toRegex()
            processed = regexSub.replace(processed) {
                "*".repeat(badWord.length)
            }
        }

        return processed
    }
}
