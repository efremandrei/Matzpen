package com.efremandrei.matzpen

enum class QuestionDepth(val id: String, val questionCount: Int) {
    QUICK("quick", 10),
    BALANCED("balanced", 14),
    FULL("full", 18);

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id }
    }
}

/** Fixed, nested sets keep a shorter session reproducible and make later questions additive. */
object QuestionPlan {
    private val orderedIds = listOf(
        // Quick: security, institutions, religion and state, economy, territory, and civil rights.
        "palestinian-state", "haredi-draft", "judicial-reform", "oct7-inquiry",
        "religion-state", "gaza-control", "lower-taxes", "price-controls",
        "settlements", "lgbt-equality",
        // Balanced: additional education, governance, economic, and diplomatic issues.
        "yeshiva-budgets", "term-limits", "food-imports", "saudi-normalization",
        // Full: all remaining policy questions.
        "death-penalty", "temple-mount", "gaza-resettlement", "universal-service"
    )

    fun supports(data: ElectionData): Boolean =
        data.questions.size == orderedIds.size && data.questions.map { it.id }.toSet() == orderedIds.toSet()

    fun questions(data: ElectionData, depth: QuestionDepth): List<Question> {
        if (!supports(data)) return data.questions
        val byId = data.questions.associateBy { it.id }
        return orderedIds.take(depth.questionCount).map { byId.getValue(it) }
    }
}
