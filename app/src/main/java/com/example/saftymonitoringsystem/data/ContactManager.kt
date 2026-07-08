package com.example.saftymonitoringsystem.data

import android.content.Context
import com.example.saftymonitoringsystem.data.model.EmergencyContact
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent storage of [EmergencyContact] records using SharedPreferences.
 * Serialises contacts as a JSON array.
 */
class ContactManager(context: Context) {

    private val prefs = context.getSharedPreferences("safety_contacts", Context.MODE_PRIVATE)

    // ── Read ──────────────────────────────────────────────────────────────────

    fun getContacts(): List<EmergencyContact> {
        val jsonString = prefs.getString("contacts_list", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val contacts = mutableListOf<EmergencyContact>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            contacts.add(
                EmergencyContact(
                    id           = obj.getString("id"),
                    name         = obj.getString("name"),
                    phone        = obj.getString("phone"),
                    email        = obj.optString("email", ""),
                    relationship = obj.getString("relationship"),
                    isPrimary    = obj.optBoolean("isPrimary", false)
                )
            )
        }
        // Primary contacts first, then alphabetical
        return contacts.sortedWith(compareByDescending { it.isPrimary })
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    fun saveContact(contact: EmergencyContact) {
        saveList(getContacts().toMutableList().also { it.add(contact) })
    }

    fun updateContact(contact: EmergencyContact) {
        val contacts = getContacts().map { if (it.id == contact.id) contact else it }
        saveList(contacts)
    }

    fun deleteContact(id: String) {
        saveList(getContacts().filter { it.id != id })
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun saveList(contacts: List<EmergencyContact>) {
        val jsonArray = JSONArray()
        contacts.forEach { c ->
            jsonArray.put(JSONObject().apply {
                put("id",           c.id)
                put("name",         c.name)
                put("phone",        c.phone)
                put("email",        c.email)
                put("relationship", c.relationship)
                put("isPrimary",    c.isPrimary)
            })
        }
        prefs.edit().putString("contacts_list", jsonArray.toString()).apply()
    }
}
