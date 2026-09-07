package com.example.data.local

import androidx.room.*
import com.example.data.local.entities.*

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUser(userId: String)

    @Query("SELECT * FROM users ORDER BY lastLogin DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("UPDATE users SET lastLogin = :time WHERE userId = :userId")
    suspend fun updateLastLogin(userId: String, time: Long)
}

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys WHERE userId = :userId AND serviceName = :serviceName")
    suspend fun getApiKey(userId: String, serviceName: String): ApiKeyEntity?

    @Query("SELECT * FROM api_keys WHERE userId = :userId")
    suspend fun getApiKeys(userId: String): List<ApiKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(apiKey: ApiKeyEntity)

    @Update
    suspend fun updateApiKey(apiKey: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE id = :keyId")
    suspend fun deleteApiKey(keyId: String)

    @Query("DELETE FROM api_keys WHERE userId = :userId")
    suspend fun deleteAllApiKeys(userId: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM user_sessions WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveSession(userId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("UPDATE user_sessions SET isActive = 0 WHERE userId = :userId")
    suspend fun invalidateAllSessions(userId: String)
}

@Dao
interface WelcomeMessageDao {
    @Query("SELECT * FROM welcome_messages WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getWelcomeMessages(userId: String): List<WelcomeMessageEntity>

    @Query("SELECT * FROM welcome_messages WHERE userId = :userId AND type = 'welcome' LIMIT 1")
    suspend fun getWelcomeMessage(userId: String): WelcomeMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWelcomeMessage(message: WelcomeMessageEntity)

    @Query("DELETE FROM welcome_messages WHERE userId = :userId")
    suspend fun deleteAllWelcomeMessages(userId: String)
}

@Dao
interface UserActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: UserActivityEntity)

    @Query("SELECT * FROM user_activity WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentActivities(userId: String, limit: Int): List<UserActivityEntity>
}
