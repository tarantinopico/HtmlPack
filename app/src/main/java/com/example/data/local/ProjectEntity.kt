package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: String, // comma-separated
    val entryPoint: String,
    val iconForeground: String,
    val iconBackground: String,
    val iconInitialLetter: String,
    val iconEmoji: String,
    val fullscreenMode: String,
    val orientation: String,
    val isPinned: Boolean,
    val hasPinLock: Boolean,
    val isOfflineOnly: Boolean,
    val fileCount: Int = 1
)
