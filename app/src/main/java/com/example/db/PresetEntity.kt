package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val speedHz: Float,
    val pattern: String,
    val timestamp: Long = System.currentTimeMillis()
)
