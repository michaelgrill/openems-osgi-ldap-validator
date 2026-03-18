package io.michael.osgildapfilter.osgildapfilter

import ai.grazie.nlp.utils.length

data class SearchElement(
    val keyStartIndex: Int, // inclusive
    val keyEndIndex: Int, // exclusive
    val equalIndex: Int,  // exclusive
    val valueStartIndex: Int, // inclusive
    val valueEndIndex: Int, // exclusive
    val key: String,
    val value: SearchElementValue,
)

sealed interface SearchElementValue {
    data class Constant(val value: String) : SearchElementValue
    data class Expression(
        val expressionPrefix: String,
        val expressionSuffix: String,
        val expressionSeparator: String,
        val variableTopic: String,
        val variableName: String
    ) : SearchElementValue
}

data class BadCharacters(
    val message: String,
    val range: IntRange,
)

class LdapFilter(
    val searchElements: List<SearchElement>,
    val badCharacters: List<BadCharacters>,
)

class LdapFilterParser(private val input: String) {

    private var pos = 0
    private val searchElements = mutableListOf<SearchElement>()
    private val badCharacters = mutableListOf<BadCharacters>()

    fun validate(): LdapFilter {
        try {
            parseFilter()
            skipWhitespace()
            if (pos < input.length) {
                badCharacters.add(BadCharacters("Unexpected trailing characters", IntRange(pos, input.length - 1)))
            }

            return LdapFilter(
                searchElements = searchElements,
                badCharacters = badCharacters,
            )
        } catch (e: Exception) {
            // fallback parsing error
            return LdapFilter(
                searchElements = emptyList(),
                badCharacters = listOf(
                    BadCharacters(
                        "Unable to parse ldap filter", IntRange(0, input.length - 1)
                    ),
                )
            )
        }
    }

    private fun parseFilter() {
        skipWhitespace()
        expect('(')

        skipWhitespace()
        when (peek()) {
            '&', '|' -> parseAndOr()
            '!' -> parseNot()
            else -> parseItem()
        }

        skipWhitespace()
        expect(')')
    }

    private fun parseAndOr() {
        pos++ // consume & or |
        skipWhitespace()

        var found = false
        while (peek() == '(') {
            parseFilter()
            found = true
            skipWhitespace()
        }

        if (!found) {
            badCharacters.add(BadCharacters("Expected at least one sub-filter", IntRange(pos, pos)))
            pos++
        }
    }

    private fun parseNot() {
        pos++ // consume !
        skipWhitespace()
        parseFilter()
    }

    private fun parseItem() {
        if (pos > input.length) {
            return
        }

        val keyStartIndex = pos
        val attr = parseAttribute()
        val keyEndIndex = pos

        val start = pos
        while (peek() != '=' && peek() != null) {
            pos++
        }
        val equalIndex = pos

        val end = pos
        if (start != end) {
            badCharacters.add(BadCharacters("Expected '=' after attribute", IntRange(start, end)))
        }

        pos++
        val valueStartIndex = pos
        val value = parseValue()
        val valueEndIndex = pos

        searchElements.add(
            SearchElement(
                keyStartIndex = keyStartIndex,
                keyEndIndex = keyEndIndex,
                equalIndex = equalIndex,
                valueStartIndex = valueStartIndex,
                valueEndIndex = valueEndIndex,
                key = attr,
                value = parseVariable(value),
            )
        )

    }

    private fun parseAttribute(): String {
        if (pos > input.length) {
            return ""
        }

        val range = skip { c -> c.isLetterOrDigit() || c == '-' || c == '.' }

        if (range.length == 0) {
            badCharacters.add(BadCharacters("Invalid attribute", range))
            return ""
        }

        return input.substring(range)
    }

    private fun parseValue(): String {
        if (pos > input.length) {
            return ""
        }

        val start = pos
        while (pos < input.length) {
            val c = input[pos]

            if (c == ')') {
                return input.substring(start, pos)
            }

            if (c == '\\') {
                pos += 3
                continue
            }

            pos++
        }

        badCharacters.add(BadCharacters("Unterminated value", IntRange(pos, pos)))
        pos++
        return input.substring(start, pos)
    }

    private fun peek(): Char? =
        if (pos < input.length) input[pos] else null

    private fun expect(c: Char) {
        val range = skip { c != it }
        pos++
        if (range.length > 0) {
            badCharacters.add(BadCharacters("Expected '$c'", range))
        }
    }

    private fun skipWhitespace() {
        val range = skip { c -> c.isWhitespace() }
        if (range.length > 0) {
            badCharacters.add(BadCharacters("Remove whitespaces", range))
        }
    }

    private fun skip(condition: (c: Char) -> Boolean): IntRange {
        if (pos > input.length) {
            return input.length..<input.length
        }

        val start = pos
        while (pos < input.length) {
            val c = input[pos]
            if (condition(c)) {
                pos++
            } else {
                break
            }
        }
        val end = pos
        return start..<end
    }

    private fun parseVariable(s: String): SearchElementValue {
        if (s.startsWith(EXPRESSION_VARIABLE_PREFIX) && s.endsWith(EXPRESSION_VARIABLE_SUFFIX)) {

            val content = s.substring(EXPRESSION_VARIABLE_PREFIX.length, s.length - EXPRESSION_VARIABLE_SUFFIX.length)
            val parts = content.split(EXPRESSION_VARIABLE_SEPARATOR)
            return SearchElementValue.Expression(
                expressionPrefix = EXPRESSION_VARIABLE_PREFIX,
                expressionSuffix = EXPRESSION_VARIABLE_SUFFIX,
                expressionSeparator = EXPRESSION_VARIABLE_SEPARATOR,
                variableTopic = parts[0],
                variableName = parts[1],
            )
        }

        return SearchElementValue.Constant(s)
    }

}

fun validateLdapFilter(filter: String): LdapFilter {
    return LdapFilterParser(filter).validate()
}
