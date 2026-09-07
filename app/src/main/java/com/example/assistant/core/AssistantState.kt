package com.example.assistant.core

import androidx.compose.ui.graphics.Color

enum class AssistantState(val label: String, val primaryColor: Color) {
    IDLE("Idle", Color(0xFF00F0FF)),
    LISTENING("Listening...", Color(0xFF10B981)),
    THINKING("Thinking...", Color(0xFFF59E0B)),
    EXECUTING("Executing...", Color(0xFF6366F1)),
    SPEAKING("Speaking...", Color(0xFF06B6D4)),
    ERROR("Error", Color(0xFFEF4444)),
    OFFLINE("Offline", Color(0xFF64748B))
}

enum class OrbTheme(
    val title: String,
    val coreColor: Color,
    val glowColor: Color,
    val ringColor: Color,
    val particleColor: Color
) {
    CYBER_CYAN(
        "Cyber Cyan",
        Color(0xFF00F0FF),
        Color(0xFF0284C7),
        Color(0xFF38BDF8),
        Color(0xFF7DD3FC)
    ),
    NEBULA_PURPLE(
        "Nebula Purple",
        Color(0xFFA855F7),
        Color(0xFF7C3AED),
        Color(0xFFC084FC),
        Color(0xFFE9D5FF)
    ),
    AURORA_GREEN(
        "Aurora Green",
        Color(0xFF10B981),
        Color(0xFF059669),
        Color(0xFF34D399),
        Color(0xFFA7F3D0)
    ),
    SOLAR_GOLD(
        "Solar Gold",
        Color(0xFFF59E0B),
        Color(0xFFD97706),
        Color(0xFFFCD34D),
        Color(0xFFFEF3C7)
    ),
    TITANIUM_DARK(
        "Titanium Slate",
        Color(0xFF94A3B8),
        Color(0xFF475569),
        Color(0xFFCBD5E1),
        Color(0xFFF1F5F9)
    )
}
