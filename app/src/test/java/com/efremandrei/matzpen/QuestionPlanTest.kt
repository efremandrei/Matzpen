package com.efremandrei.matzpen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionPlanTest {
    private val ids = listOf(
        "palestinian-state", "death-penalty", "haredi-draft", "judicial-reform",
        "oct7-inquiry", "religion-state", "gaza-control", "lower-taxes",
        "yeshiva-budgets", "price-controls", "temple-mount", "settlements",
        "term-limits", "gaza-resettlement", "food-imports", "lgbt-equality",
        "saudi-normalization", "universal-service"
    )
    private val data = ElectionData(1, "", "", ids.map { Question(it, "", "", "", "https://example.org") }, emptyList(), emptyMap())

    @Test fun `depths are nested and cover every question once`() {
        val quick = QuestionPlan.questions(data, QuestionDepth.QUICK).map { it.id }
        val balanced = QuestionPlan.questions(data, QuestionDepth.BALANCED).map { it.id }
        val full = QuestionPlan.questions(data, QuestionDepth.FULL).map { it.id }
        assertTrue(QuestionPlan.supports(data))
        assertEquals(10, quick.size)
        assertEquals(14, balanced.size)
        assertEquals(18, full.size)
        assertEquals(quick, balanced.take(10))
        assertEquals(balanced, full.take(14))
        assertEquals(ids.toSet(), full.toSet())
    }

    @Test fun `changed question set falls back to source order`() {
        val changed = data.copy(questions = data.questions.dropLast(1))
        assertFalse(QuestionPlan.supports(changed))
        assertEquals(changed.questions, QuestionPlan.questions(changed, QuestionDepth.QUICK))
    }
}
