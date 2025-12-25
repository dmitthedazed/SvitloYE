package com.occaecat.ztoeschedule.data.model

enum class DisplayMode {
    COMPACT,      // Щільний - більше інформації
    COMFORTABLE,  // Звичайний (стандарт)
    SPACIOUS      // Просторий - великі елементи
}

enum class ColorTheme {
    SYSTEM,       // Системна
    LIGHT,        // Світла
    DARK,         // Темна
    AMOLED,       // Чорна (економія)
    CONTRAST      // Високий контраст
}

enum class FontScale(val multiplier: Float) {
    SMALL(0.85f),
    NORMAL(1.0f),
    LARGE(1.15f),
    XLARGE(1.3f),
    ACCESSIBILITY(1.5f)
}