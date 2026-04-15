package io.michael.osgildapfilter.osgildapfilter

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail


class LdapFilterTest {

    @Test
    fun validateValidFilter() {
        val filter = validateLdapFilter("(&(enabled=true)(!(service.factoryPid=\${config.alias})))")

        assertTrue(filter.badCharacters.isEmpty()) { "Unexpected bad characters at ${filter.badCharacters}" }
    }

    @Test
    fun validateValidFilterDefaultIdWithEnabled() {
        val filter = validateLdapFilter("(&(id=\${config.modbus_id})(enabled=true))")

        assertEquals(0, filter.badCharacters.size, "Unexpected bad characters at ${filter.badCharacters}")
    }

    @Test
    fun validateVariable() {
        val filter = validateLdapFilter("(enabled=true)")

        assertEquals(1, filter.searchElements.size)
        assertEquals("enabled", filter.searchElements.first().key)
        val value = filter.searchElements.first().value
        if (value !is SearchElementValue.Constant) {
            fail()
        }
        assertEquals("true", value.value)
    }

    @Test
    fun validateVariableExpression() {
        val filter = validateLdapFilter("(service.factoryPid=\${config.alias})")

        assertEquals(1, filter.searchElements.size)
        assertEquals("service.factoryPid", filter.searchElements.first().key)
        val value = filter.searchElements.first().value
        if (value !is SearchElementValue.Expression) {
            fail()
        }
        assertEquals("\${", value.expressionPrefix)
        assertEquals("config", value.variableTopic)
        assertEquals(".", value.expressionSeparator)
        assertEquals("alias", value.variableName)
        assertEquals("}", value.expressionSuffix)
    }

    @Test
    fun validateEmptyString() {
        val filter = validateLdapFilter("   ")

        assertEquals(1, filter.badCharacters.size, "Unexpected bad characters at ${filter.badCharacters}")
        assertEquals(0..<3, filter.badCharacters.first().range)
        assertTrue(filter.searchElements.isEmpty()) { "Unexpected search elements ${filter.searchElements}" }
    }

    @Test
    fun validateRandomSpaces() {
        val filter = validateLdapFilter("(&(enabled=true)   (!(service.factoryPid=\${config.alias})))")

        assertEquals(1, filter.badCharacters.size, "Unexpected bad characters at ${filter.badCharacters}")
        assertEquals(16..<19, filter.badCharacters.first().range)
    }

    @Test
    fun validateLastBracket() {
        val filter = validateLdapFilter("(&(id=component0)(enabled=true)")

        assertEquals(1, filter.badCharacters.size, "Unexpected bad characters at ${filter.badCharacters}")
        assertEquals(31..31, filter.badCharacters.first().range)
    }

}