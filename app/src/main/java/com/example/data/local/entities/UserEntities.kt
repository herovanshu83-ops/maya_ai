package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String = UUID.randomUUID().toString(),
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val profilePicture: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var lastLogin: Long = System.currentTimeMillis(),
    val isPremium: Boolean = false,
    val userType: String = "free",
    var preferences: String = "{}"
)

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val serviceName: String,
    var encryptedKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    var lastUsed: Long = System.currentTimeMillis(),
    var isActive: Boolean = true
)

@Entity(tableName = "user_sessions")
data class SessionEntity(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val token: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
    var isActive: Boolean = true
)

@Entity(tableName = "welcome_messages")
data class WelcomeMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val message: String,
    val type: String = "welcome",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    var lastShown: Long? = null
)

@Entity(tableName = "user_activity")
data class UserActivityEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val action: String,
    val details: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)
