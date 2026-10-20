package com.victorious.cam.xposed

/**
 * Hardcoded denylist: never hook banking, payment, government ID, or KYC/identity apps.
 * Matching is by exact package name (lowercase) and simple category keywords in the package.
 */
object HookDenylist {

    /** Exact package names known to be banking / payments / gov ID / KYC. */
    val EXACT_PACKAGES: Set<String> = setOf(
        // Payments / wallets
        "com.paypal.android.p2pmobile",
        "com.venmo",
        "com.squareup.cash",
        "com.google.android.apps.walletnfcrel",
        "com.apple.android.pay",
        "com.chase.sig.android",
        "com.bankofamerica.cashpros",
        "com.wf.wellsfargomobile",
        "com.citi.citimobile",
        "com.infonow.bofa",
        "com.usbank.mobilebanking",
        "com.capitalone.mobile",
        "com.americanexpress.android.acctsvcs.us",
        "com.s2gastoos.softwares2g.bbva.android",
        // Government / ID
        "gov.irs",
        "gov.usa",
        "com.usps.myusps",
        "com.eid.eid",
        // KYC / identity verification vendors (common SDKs / apps)
        "com.jumio.nvmobile",
        "com.onfido.android",
        "com.veriff.sdk",
        "com.socure",
        "com.persona.sdk",
        "com.alloy.android",
        "com.sumsub.sentry",
        "com.idology",
        "com.aureus.kyc"
    )

    /** Substrings / category tokens that force a deny when found in the package name. */
    val CATEGORY_TOKENS: List<String> = listOf(
        "bank", "banking", "wallet", "payment", "paypal", "venmo", "cashapp",
        "wellsfargo", "chase", "citibank", "capitalone", "americanexpress", "amex",
        "gov.", "irs", "dmv", "passport", "nationalid",
        "kyc", "onfido", "jumio", "veriff", "socure", "persona", "sumsub",
        "biometric", "identityverif", "idverify", "idcheck"
    )

    fun isDenied(packageName: String): Boolean {
        val pkg = packageName.lowercase().trim()
        if (pkg.isEmpty()) return true
        if (pkg in EXACT_PACKAGES) return true
        return CATEGORY_TOKENS.any { token -> pkg.contains(token) }
    }

    fun denyReason(packageName: String): String? {
        val pkg = packageName.lowercase().trim()
        if (pkg in EXACT_PACKAGES) return "exact denylist package"
        val hit = CATEGORY_TOKENS.firstOrNull { pkg.contains(it) } ?: return null
        return "category token '$hit'"
    }
}
