package com.example.serviciosya.domain.usecase

import com.example.serviciosya.testutil.testCategory
import com.example.serviciosya.testutil.testProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchServicesUseCaseTest {
    private val search = SearchServicesUseCase()

    private val categories = listOf(
        testCategory(id = "electricistas", name = "Electricista"),
        testCategory(id = "plomeros", name = "Plomero"),
        testCategory(id = "tecnicos-pc", name = "Técnico de PC"),
    )
    private val providers = listOf(
        testProvider(id = "p1", name = "Carlos Electricidad"),
        testProvider(id = "p2", name = "Plomería García", categoryId = "plomeros"),
        testProvider(id = "p3", name = "ElectroFix"),
    )

    @Test
    fun `matches category and provider names partially`() {
        val result = search("electr", categories, providers)

        assertEquals(listOf("electricistas"), result.categories.map { it.id })
        assertEquals(listOf("p1", "p3"), result.providers.map { it.id })
    }

    @Test
    fun `ignores case accents and surrounding spaces`() {
        assertEquals(listOf("tecnicos-pc"), search("  TECNICO  ", categories, providers).categories.map { it.id })
        assertEquals(listOf("p2"), search("plomeria", categories, providers).providers.map { it.id })
        assertEquals(listOf("p2"), search("GARCÍA", categories, providers).providers.map { it.id })
    }

    @Test
    fun `collapses repeated whitespace inside the query`() {
        assertEquals(listOf("tecnicos-pc"), search("tecnico   de pc", categories, providers).categories.map { it.id })
    }

    @Test
    fun `blank query returns nothing`() {
        assertTrue(search("   ", categories, providers).isEmpty)
    }

    @Test
    fun `no matches returns an empty result`() {
        assertTrue(search("cerrajero", categories, providers).isEmpty)
    }
}
