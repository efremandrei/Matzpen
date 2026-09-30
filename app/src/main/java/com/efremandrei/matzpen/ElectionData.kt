package com.efremandrei.matzpen

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

data class Question(val id: String, val he: String, val en: String, val ar: String, val sourceUrl: String) {
    fun text(lang: String) = when (lang) { "he" -> he; "ar" -> ar; else -> en }
}

data class ElectionList(
    val id: String, val he: String, val en: String, val ballot: String,
    val status: String, val sourceUrl: String
) {
    fun name(lang: String) = if (lang == "he") he else en
}

data class Position(
    val listId: String, val questionId: String, val value: Int,
    val sourceType: String, val sourceTitle: String, val sourceDate: String,
    val sourceUrl: String
)

data class ElectionData(
    val revision: Int, val updatedAt: String, val sourceVersion: String,
    val questions: List<Question>, val lists: List<ElectionList>,
    val positions: Map<Pair<String, String>, Position>
) {
    companion object {
        fun parse(raw: String): ElectionData {
            val root = JSONObject(raw)
            require(root.getInt("schemaVersion") == 1)
            require(root.getString("electionId") == "il-knesset-26")
            val questions = root.getJSONArray("questions").let { arr ->
                (0 until arr.length()).map { i ->
                    arr.getJSONObject(i).let { q -> Question(q.getString("id"), q.getString("he"), q.getString("en"), q.getString("ar"), q.getString("sourceUrl")) }
                }
            }
            val lists = root.getJSONArray("lists").let { arr ->
                (0 until arr.length()).map { i ->
                    arr.getJSONObject(i).let { p -> ElectionList(p.getString("id"), p.getString("he"), p.getString("en"), p.getString("ballot"), p.getString("status"), p.getString("sourceUrl")) }
                }
            }
            val positions = root.getJSONArray("positions").let { arr ->
                (0 until arr.length()).map { i ->
                    arr.getJSONObject(i).let { p -> Position(
                        p.getString("listId"), p.getString("questionId"), p.getInt("value"),
                        p.getString("sourceType"), p.getString("sourceTitle"), p.optString("sourceDate", ""),
                        p.getString("sourceUrl")
                    ) }
                }.associateBy { it.listId to it.questionId }
            }
            require(questions.size == 18 && lists.size == 38)
            require(questions.map { it.id }.distinct().size == 18)
            require(lists.map { it.id }.distinct().size == 38)
            require(positions.values.all { it.value in -2..2 && it.sourceUrl.startsWith("https://") })
            val questionIds = questions.map { it.id }.toSet()
            val listIds = lists.map { it.id }.toSet()
            require(positions.values.all { it.questionId in questionIds && it.listId in listIds })
            return ElectionData(root.getInt("revision"), root.getString("updatedAt"), root.getString("sourceVersion"), questions, lists, positions)
        }
    }
}

data class VoterAnswer(val value: Int, val priority: Boolean)
data class MatchResult(val list: ElectionList, val score: Int?, val coverage: Double, val known: Int)

object ScoreEngine {
    fun results(data: ElectionData, answers: Map<String, VoterAnswer>): List<MatchResult> {
        val valid = data.questions.mapNotNull { q -> answers[q.id]?.let { q.id to it } }
        val totalWeight = valid.fold(0) { total, (_, answer) -> total + if (answer.priority) 2 else 1 }
        return data.lists.map { list ->
            var knownWeight = 0
            var points = 0.0
            var known = 0
            for ((id, answer) in valid) {
                val position = data.positions[list.id to id] ?: continue
                val weight = if (answer.priority) 2 else 1
                knownWeight += weight
                known++
                points += weight * (1.0 - abs(answer.value - position.value) / 4.0)
            }
            val coverage = if (totalWeight == 0) 0.0 else knownWeight.toDouble() / totalWeight
            val score = if (valid.size >= 8 && coverage >= 0.7 && knownWeight > 0)
                (points / knownWeight * 100).roundToInt() else null
            MatchResult(list, score, coverage, known)
        }.sortedWith(compareBy<MatchResult> { it.score == null }.thenByDescending { it.score ?: -1 }.thenBy { it.list.he })
    }
}
