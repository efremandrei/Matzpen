package com.efremandrei.matzpen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreEngineTest {
    private val questions = (1..18).map { Question("q$it", "", "", "", "https://example.org") }
    private val lists = listOf(
        ElectionList("a", "A", "A", "א", "committee_approved", "https://example.org"),
        ElectionList("b", "B", "B", "ב", "committee_approved", "https://example.org")
    )

    @Test fun `eight answers and sufficient evidence produce score`() {
        val positions = (1..8).associate { ("a" to "q$it") to Position("a", "q$it", 2, "platform", "", "", "https://example.org") }
        val data = ElectionData(1, "", "", questions, lists, positions)
        val answers = (1..8).associate { "q$it" to VoterAnswer(2, false) }
        val results = ScoreEngine.results(data, answers)
        assertEquals(100, results.first { it.list.id == "a" }.score)
        assertNull(results.first { it.list.id == "b" }.score)
    }

    @Test fun `coverage threshold prevents sparse list ranking`() {
        val positions = (1..6).associate { ("a" to "q$it") to Position("a", "q$it", 2, "platform", "", "", "https://example.org") }
        val data = ElectionData(1, "", "", questions, lists, positions)
        val answers = (1..10).associate { "q$it" to VoterAnswer(2, false) }
        assertNull(ScoreEngine.results(data, answers).first { it.list.id == "a" }.score)
    }

    @Test fun `priority doubles impact`() {
        val positions = (1..8).associate { ("a" to "q$it") to Position("a", "q$it", if (it == 1) -2 else 2, "platform", "", "", "https://example.org") }
        val data = ElectionData(1, "", "", questions, lists, positions)
        val ordinary = (1..8).associate { "q$it" to VoterAnswer(2, false) }
        val prioritized = ordinary.toMutableMap().apply { this["q1"] = VoterAnswer(2, true) }
        assertEquals(88, ScoreEngine.results(data, ordinary).first { it.list.id == "a" }.score)
        assertEquals(78, ScoreEngine.results(data, prioritized).first { it.list.id == "a" }.score)
    }

    @Test fun `long path still requires eight sourced positions`() {
        val fifty = (1..50).map { Question("q$it", "", "", "", "https://example.org") }
        val answers = (1..50).associate { "q$it" to VoterAnswer(2, false) }
        val sparse = (1..7).associate { ("a" to "q$it") to Position("a", "q$it", 2, "platform", "", "", "https://example.org") }
        val sufficient = (1..20).associate { ("b" to "q$it") to Position("b", "q$it", 2, "platform", "", "", "https://example.org") }
        val data = ElectionData(2, "", "", fifty, lists, sparse + sufficient)
        val results = ScoreEngine.results(data, answers)
        assertNull(results.first { it.list.id == "a" }.score)
        assertEquals(100, results.first { it.list.id == "b" }.score)
    }
}
