package com.example.model

import java.util.UUID

enum class UserRole {
    CLIENT,
    SUB_ADMIN,
    OWNER
}

data class SubAdminPermissions(
    val canViewOtherUsersData: Boolean = false,
    val canViewEditFormulas: Boolean = false,
    val canCreateNewSections: Boolean = false,
    val canEditGlobalRates: Boolean = false,
    val canControlUserTrials: Boolean = false,
    val canControlUserPayments: Boolean = false,
    val canMarkLifetimeFree: Boolean = false,
    val canViewClientLocations: Boolean = false,
    val canManageRequests: Boolean = false,
    val canManageOffers: Boolean = false
) {
    companion object {
        val ALL_PERMISSIONS = SubAdminPermissions(
            canViewOtherUsersData = true,
            canViewEditFormulas = true,
            canCreateNewSections = true,
            canEditGlobalRates = true,
            canControlUserTrials = true,
            canControlUserPayments = true,
            canMarkLifetimeFree = true,
            canViewClientLocations = true,
            canManageRequests = true,
            canManageOffers = true
        )

        val DEFAULT_LIMITED = SubAdminPermissions(
            canViewOtherUsersData = true,
            canManageRequests = true
        )
    }
}

data class SubAdminUser(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val phone: String = "",
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val permissions: SubAdminPermissions = SubAdminPermissions.DEFAULT_LIMITED
)

data class ClientSupportRequest(
    val id: String = UUID.randomUUID().toString(),
    val userName: String,
    val userPhone: String,
    val userCnic: String,
    val userEmail: String = "",
    val requestType: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val resolvedBy: String = "",
    val resolvedAt: Long = 0L
)

data class AppOfferBanner(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Special Limited Offer",
    val message: String = "",
    val subtitle: String = message,
    val badgeText: String = "Special Offer",
    val discountPercent: Int = 20,
    val promoCode: String = "BOSS5M",
    val validUntilText: String = "Limited Time Offer",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "Owner"
) {
    fun isCurrentlyValid(): Boolean = isActive && title.isNotBlank()
    fun getRemainingText(): String = validUntilText.ifBlank { "Limited Time Offer" }
}

data class PromotionalOffer(
    val isActive: Boolean = false,
    val title: String = "5-Month Special Discount Package",
    val description: String = "Unlock 5 Months Full Pro Access at a special discounted rate!",
    val discountPercent: Int = 25,
    val promoCode: String = "BOSS5M",
    val validUntilText: String = "Limited Time Offer",
    val offerDurationMonths: Int = 5,
    val specialPricePkr: Int = 4500,
    val targetTierName: String = "PRO_5_MONTHS"
) {
    fun toOfferBanner(): AppOfferBanner = AppOfferBanner(
        title = title,
        message = description,
        subtitle = description,
        badgeText = "$discountPercent% OFF",
        discountPercent = discountPercent,
        promoCode = promoCode,
        validUntilText = validUntilText,
        isActive = isActive
    )
}

data class AdminAuditEntry(
    val timestampMs: Long = System.currentTimeMillis(),
    val action: String,
    val details: String
)

data class MasterAdminState(
    val promotionalOffer: PromotionalOffer = PromotionalOffer(),
    val globalCustomSections: List<String> = emptyList(),
    val globalSectionRates12mm: Map<String, Double> = emptyMap(),
    val auditLogs: List<AdminAuditEntry> = emptyList()
)
