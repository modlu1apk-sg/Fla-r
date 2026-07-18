package com.example.db

import kotlinx.coroutines.flow.Flow

class PresetRepository(private val presetDao: PresetDao) {
    val allPresets: Flow<List<PresetEntity>> = presetDao.getAllPresets()

    suspend fun insert(preset: PresetEntity) {
        presetDao.insertPreset(preset)
    }

    suspend fun delete(preset: PresetEntity) {
        presetDao.deletePreset(preset)
    }

    suspend fun deleteById(id: Int) {
        presetDao.deletePresetById(id)
    }
}
