package com.example.christmastrees.data

enum class MarkerType(
    val label: String,
    val emoji: String,
    val category: Category
) {
    CHRISTMAS_TREE("Ёлка", "🎄", Category.TREES),
    SNOWMAN("Снеговик", "⛄", Category.SNOWMEN),
    CITY_DECORATION("Украшения", "🎊", Category.DECORATIONS);

    companion object {
        fun fromString(value: String): MarkerType {
            return values().find { it.name == value }
                ?: CHRISTMAS_TREE
        }
    }
}

enum class Category(
    val displayName: String
) {
    TREES("Ёлки"),
    SNOWMEN("Снеговики"),
    DECORATIONS("Украшения")
}
