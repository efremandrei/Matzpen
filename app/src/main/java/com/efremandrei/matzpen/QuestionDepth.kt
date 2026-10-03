package com.efremandrei.matzpen

enum class QuestionDepth(val id: String, val questionCount: Int) {
    QUICK("quick", 12),
    BALANCED("balanced", 25),
    FULL("full", 50);

    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id }
    }
}

/** Fixed, nested sets keep a shorter session reproducible and make later questions additive. */
object QuestionPlan {
    private val originalIds = listOf(
        // Quick: security, institutions, religion and state, economy, territory, and civil rights.
        "palestinian-state", "haredi-draft", "judicial-reform", "oct7-inquiry",
        "religion-state", "gaza-control", "lower-taxes", "price-controls",
        "settlements", "lgbt-equality",
        // Balanced: additional education, governance, economic, and diplomatic issues.
        "yeshiva-budgets", "term-limits", "food-imports", "saudi-normalization",
        // Full: all remaining policy questions.
        "death-penalty", "temple-mount", "gaza-resettlement", "universal-service"
    )

    private val orderedIds = listOf(
        *originalIds.take(10).toTypedArray(),
        "food-monopolies", "written-constitution",
        *originalIds.drop(10).toTypedArray(),
        "core-curriculum", "service-alternatives", "civil-marriage",
        "public-healthcare", "west-bank-sovereignty",
        "school-vouchers", "school-autonomy", "territorial-concessions", "teacher-pay",
        "early-childcare", "preventive-care", "trauma-care", "long-term-rent",
        "reservist-housing", "participatory-voting", "business-regulation", "torah-study-protection",
        "services-vat", "debt-relief", "basic-income", "fewer-ministries",
        "judicial-independence", "override-clause", "local-shabbat",
        "pluralist-conversion", "jewish-identity-education", "professional-army",
        "voting-service", "arab-community-crime", "cancel-oslo"
    )

    fun supports(data: ElectionData): Boolean =
        (data.questions.size == orderedIds.size && data.questions.map { it.id }.toSet() == orderedIds.toSet()) ||
            (data.questions.size == originalIds.size && data.questions.map { it.id }.toSet() == originalIds.toSet())

    fun questions(data: ElectionData, depth: QuestionDepth): List<Question> {
        if (!supports(data)) return data.questions
        val byId = data.questions.associateBy { it.id }
        val selected = if (data.questions.size == originalIds.size) originalIds else orderedIds
        val count = if (data.questions.size == originalIds.size) when (depth) {
            QuestionDepth.QUICK -> 10
            QuestionDepth.BALANCED -> 14
            QuestionDepth.FULL -> 18
        } else depth.questionCount
        return selected.take(count).map { byId.getValue(it) }
    }
}
