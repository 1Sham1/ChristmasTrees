package com.example.christmastrees.data

enum class MarkerType(
    val label: String,
    val emoji: String,
    val category: Category,
    val bgColor: Int,
    val borderColor: Int,
    val accentColor: Int
) {
    CHRISTMAS_TREE("Ёлка", "🎄", Category.TREES,
        android.graphics.Color.parseColor("#1B5E20"),
        android.graphics.Color.parseColor("#F2C94C"),
        android.graphics.Color.parseColor("#E53935")),
    SNOWMAN("Снеговик", "☃️", Category.SNOWMEN,
        android.graphics.Color.parseColor("#1A237E"),
        android.graphics.Color.parseColor("#B0BEC5"),
        android.graphics.Color.parseColor("#FF1744")),
    CITY_DECORATION("Украшения", "🎁", Category.DECORATIONS,
        android.graphics.Color.parseColor("#4A148C"),
        android.graphics.Color.parseColor("#FF5722"),
        android.graphics.Color.parseColor("#FFD600"));

    companion object {
        fun fromString(value: String): MarkerType {
            return values().find { it.name == value }
                ?: run {
                    android.util.Log.w("MarkerType", "Unknown marker type: '$value', defaulting to CHRISTMAS_TREE")
                    CHRISTMAS_TREE
                }
        }
    }
}

enum class Category(
    val displayName: String
) {
    TREES("Ёлки"),
    SNOWMEN("Снеговики"),
    DECORATIONS("Украшения");

    val emoji: String
        get() = when (this) {
            TREES -> "🎄"
            SNOWMEN -> "☃️"
            DECORATIONS -> "🎁"
        }
}
