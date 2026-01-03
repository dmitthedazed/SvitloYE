package com.occaecat.ztoeschedule.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.occaecat.ztoeschedule.data.model.SavedAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages storage of the saved address list using a local JSON file.
 * We use a file instead of DataStore for the list to avoid complex Protobuf setup
 * or complexity of Room for a small dataset.
 */
class AddressStorage(private val context: Context) {

    private val gson = Gson()
    private val fileName = "saved_addresses.json"

    private fun getFile(): File = File(context.filesDir, fileName)

    suspend fun getAddresses(): List<SavedAddress> = withContext(Dispatchers.IO) {
        val file = getFile()
        if (!file.exists()) return@withContext emptyList()

        return@withContext try {
            val json = file.readText()
            val type = object : TypeToken<List<SavedAddress>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateAll(list: List<SavedAddress>) = withContext(Dispatchers.IO) {
        saveAddresses(list)
    }

    suspend fun saveAddresses(list: List<SavedAddress>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(list)
        getFile().writeText(json)
    }

    suspend fun addAddress(address: SavedAddress) {
        val current = getAddresses().toMutableList()
        // Determine priority: if list is empty, priority is 1, else last + 1
        val newPriority = if (current.isEmpty()) 1 else (current.maxOfOrNull { it.priority } ?: 0) + 1
        
        current.add(address.copy(priority = newPriority))
        saveAddresses(current)
    }

    suspend fun updateAddress(address: SavedAddress) {
        val current = getAddresses().toMutableList()
        val index = current.indexOfFirst { it.id == address.id }
        if (index != -1) {
            current[index] = address
            saveAddresses(current)
        }
    }

    suspend fun deleteAddress(id: String) {
        val current = getAddresses().toMutableList()
        current.removeAll { it.id == id }
        
        // Re-normalize priorities
        val sorted = current.sortedBy { it.priority }
        val rePrioritized = sorted.mapIndexed { index, addr -> 
            addr.copy(priority = index + 1) 
        }
        
        saveAddresses(rePrioritized)
    }

    suspend fun swapPriorities(id1: String, id2: String) {
        val current = getAddresses().toMutableList()
        val index1 = current.indexOfFirst { it.id == id1 }
        val index2 = current.indexOfFirst { it.id == id2 }
        
        if (index1 != -1 && index2 != -1) {
            val addr1 = current[index1]
            val addr2 = current[index2]
            
            // Swap priority values
            current[index1] = addr1.copy(priority = addr2.priority)
            current[index2] = addr2.copy(priority = addr1.priority)
            
            saveAddresses(current)
        }
    }

    /**
     * Set the address with the given ID as the Primary (Priority 1).
     * Swaps priorities if necessary.
     */
    suspend fun setAsPrimary(id: String): List<SavedAddress> {
        val current = getAddresses().toMutableList()
        val targetIndex = current.indexOfFirst { it.id == id }
        if (targetIndex == -1) return current

        val target = current[targetIndex]
        if (target.priority == 1) return current // Already primary

        // Find current primary
        val primaryIndex = current.indexOfFirst { it.priority == 1 }
        
        if (primaryIndex != -1) {
            // Swap priorities
            val primary = current[primaryIndex]
            current[primaryIndex] = primary.copy(priority = target.priority)
            current[targetIndex] = target.copy(priority = 1)
        } else {
            // Just set target to 1
            current[targetIndex] = target.copy(priority = 1)
        }

        // Sort by priority to be clean
        val sorted = current.sortedBy { it.priority }
        saveAddresses(sorted)
        return sorted
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        val file = getFile()
        if (file.exists()) {
            file.delete()
        }
    }
}
