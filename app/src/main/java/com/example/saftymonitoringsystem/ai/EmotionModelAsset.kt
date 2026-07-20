package com.example.saftymonitoringsystem.ai

import android.content.Context

object EmotionModelAsset {
    const val DEFAULT_ASSET_NAME = "model.pth"

    fun inferAssetName(requested: String?): String {
        val trimmed = requested?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> DEFAULT_ASSET_NAME
            trimmed.contains('/') || trimmed.contains('\\') -> {
                trimmed.substringAfterLast('/').substringAfterLast('\\').trim()
            }
            else -> trimmed
        }.takeIf { it.isNotEmpty() } ?: DEFAULT_ASSET_NAME
    }

    fun exists(context: Context, requested: String? = null): Boolean {
        val assetName = inferAssetName(requested)
        return context.assets.list("")?.contains(assetName) == true
    }
}
