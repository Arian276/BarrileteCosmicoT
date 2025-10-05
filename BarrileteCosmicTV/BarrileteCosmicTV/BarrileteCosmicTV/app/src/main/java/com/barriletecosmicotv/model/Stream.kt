package com.barriletecosmicotv.model

data class Stream(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val streamUrl: String,
    val category: String,
    val isLive: Boolean,
    val viewerCount: Int,
    val country: String,
    val language: String,
    val quality: String,
    val createdAt: String,
    val updatedAt: String
)