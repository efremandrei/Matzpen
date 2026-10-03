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

    @Test fun `fifty-question plan is nested and includes small-party issues`() {
        val added = """core-curriculum school-vouchers school-autonomy territorial-concessions teacher-pay early-childcare public-healthcare preventive-care trauma-care long-term-rent reservist-housing participatory-voting food-monopolies business-regulation torah-study-protection services-vat debt-relief basic-income fewer-ministries written-constitution judicial-independence override-clause local-shabbat civil-marriage pluralist-conversion jewish-identity-education service-alternatives professional-army voting-service arab-community-crime cancel-oslo west-bank-sovereignty""".split(" ")
        val expanded = data.copy(questions = (ids + added).map { Question(it, "", "", "", "https://example.org") })
        assertTrue(QuestionPlan.supports(expanded))
        val quick = QuestionPlan.questions(expanded, QuestionDepth.QUICK).map { it.id }
        val balanced = QuestionPlan.questions(expanded, QuestionDepth.BALANCED).map { it.id }
        val full = QuestionPlan.questions(expanded, QuestionDepth.FULL).map { it.id }
        assertEquals(12, quick.size)
        assertEquals(25, balanced.size)
        assertEquals(50, full.size)
        assertEquals(quick, balanced.take(12))
        assertEquals(balanced, full.take(25))
        assertEquals((ids + added).toSet(), full.toSet())
    }
}
