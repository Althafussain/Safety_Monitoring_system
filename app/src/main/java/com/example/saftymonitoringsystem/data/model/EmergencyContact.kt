package com.example.saftymonitoringsystem.data.model

/**
 * Represents a trusted emergency contact stored locally on the device.
 *
 * @property id           Unique identifier (UUID)
 * @property name         Contact's full name
 * @property phone        Phone number for SMS alerts
 * @property email        Email address for email alerts (optional)
 * @property relationship Relationship to user (e.g. "Mother", "Friend")
 * @property isPrimary    Whether this contact is the highest-priority contact
 */
data class EmergencyContact(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val email: String = "",
    val relationship: String = "",
    val isPrimary: Boolean = false
)
