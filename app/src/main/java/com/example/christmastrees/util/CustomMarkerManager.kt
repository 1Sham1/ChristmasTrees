package com.example.christmastrees.util

import android.content.Context
import android.util.Log
import com.example.christmastrees.data.CustomMarkerType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CustomMarkerManager {

    private const val PREFS_SUFFIX = "custom_marker_types"
    private val gson = Gson()

    private fun getPrefsKey(profileName: String) = "${PREFS_SUFFIX}_$profileName"

    fun saveCustomTypes(context: Context, profileName: String, types: List<CustomMarkerType>) {
        try {
            val prefs = context.getSharedPreferences(getPrefsKey(profileName), Context.MODE_PRIVATE)
            val json = gson.toJson(types)
            prefs.edit().putString("custom_types_json", json).apply()
            Log.d("CustomMarkerManager", "Saved ${types.size} custom types for profile '$profileName'")
        } catch (e: Exception) {
            Log.e("CustomMarkerManager", "Failed to save custom types", e)
        }
    }

    fun loadCustomTypes(context: Context, profileName: String): List<CustomMarkerType> {
        try {
            val prefs = context.getSharedPreferences(getPrefsKey(profileName), Context.MODE_PRIVATE)
            val json = prefs.getString("custom_types_json", null)
            if (json.isNullOrEmpty()) return emptyList()

            val type = object : TypeToken<List<CustomMarkerType>>() {}.type
            val types: List<CustomMarkerType> = gson.fromJson(json, type) ?: emptyList()
            Log.d("CustomMarkerManager", "Loaded ${types.size} custom types for profile '$profileName'")
            return types
        } catch (e: Exception) {
            Log.e("CustomMarkerManager", "Failed to load custom types", e)
            return emptyList()
        }
    }

    fun addCustomType(context: Context, profileName: String, customType: CustomMarkerType) {
        val types = loadCustomTypes(context, profileName).toMutableList()
        types.add(customType)
        saveCustomTypes(context, profileName, types)
    }

    fun deleteCustomType(context: Context, profileName: String, id: String) {
        val types = loadCustomTypes(context, profileName).filter { it.id != id }.toMutableList()
        saveCustomTypes(context, profileName, types)
        Log.d("CustomMarkerManager", "Deleted custom type '$id' for profile '$profileName'")
    }

    fun updateCustomType(context: Context, profileName: String, customType: CustomMarkerType) {
        val types = loadCustomTypes(context, profileName).toMutableList()
        val idx = types.indexOfFirst { it.id == customType.id }
        if (idx != -1) {
            types[idx] = customType
            saveCustomTypes(context, profileName, types)
        }
    }

    fun getCustomType(context: Context, profileName: String, id: String): CustomMarkerType? {
        return loadCustomTypes(context, profileName).find { it.id == id }
    }

    fun clearProfile(context: Context, profileName: String) {
        try {
            val prefs = context.getSharedPreferences(getPrefsKey(profileName), Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d("CustomMarkerManager", "Cleared custom types for profile '$profileName'")
        } catch (e: Exception) {
            Log.e("CustomMarkerManager", "Failed to clear custom types", e)
        }
    }
}
