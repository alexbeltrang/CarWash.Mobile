package com.carwash.mobile.ingreso

import java.util.Locale

fun reviewedPlate(value: String): String? = value.trim().uppercase(Locale.ROOT).takeIf { it.isNotBlank() }

fun plateCandidates(text: String): List<String> {
    val words = Regex("[A-Z0-9]+")
    val lines = text.uppercase(Locale.ROOT).lineSequence()
        .map { line -> words.findAll(line).map { it.value }.toList() }
        .filter { it.isNotEmpty() }.toList()
    return buildList {
        fun suggest(candidate: String) {
            if (candidate.length in 5..10 && candidate.any { it in 'A'..'Z' } && candidate.any { it in '0'..'9' }) {
                add(candidate)
            }
        }
        lines.forEach { tokens ->
            tokens.forEach(::suggest)
            for (count in 2..3) tokens.windowed(count).forEach { suggest(it.joinToString("")) }
        }
        lines.zipWithNext().forEach { (first, second) ->
            val letters = first.joinToString("")
            val suffix = second.joinToString("")
            if (letters.all { it in 'A'..'Z' } && suffix.first().isDigit()) suggest(letters + suffix)
        }
    }.distinct().take(8)
}