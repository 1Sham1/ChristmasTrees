package com.example.christmastrees.data

enum class MarkerSize(
    val label: String,
    val quantityMultiplier: Float,
    val iconScale: Float,
    val iconSize: Int,
    val emojiTextSize: Int,
    val borderColor: Int,
    val badgeColor: Int
) {
    SMALL(
        "Мелкий",
        0.1f,
        0.7f,
        70,
        40,
        android.graphics.Color.parseColor("#B0BEC5"),
        android.graphics.Color.parseColor("#546E7A")
    ),
    NORMAL(
        "Обычный",
        1.0f,
        1.0f,
        100,
        56,
        android.graphics.Color.parseColor("#FFFFFF"),
        android.graphics.Color.parseColor("#C62828")
    ),
    BOSS(
        "Босс",
        1.0f,
        1.5f,
        150,
        72,
        android.graphics.Color.parseColor("#FFD700"),
        android.graphics.Color.parseColor("#FF6F00")
    );
}
