package com.efremandrei.matzpen

import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

data class Question(val id: String, val he: String, val en: String, val ar: String, val sourceUrl: String, val ru: String? = null) {
    fun text(lang: String) = when (lang) { "he" -> he; "ar" -> ar; "ru" -> ru ?: en; else -> en }
}

data class ElectionList(
    val id: String, val he: String, val en: String, val ballot: String,
    val status: String, val sourceUrl: String, val ru: String? = null
) {
    fun name(lang: String) = when (lang) { "he" -> he; "ru" -> ru ?: en; else -> en }
}

data class Position(
    val listId: String, val questionId: String, val value: Int,
    val sourceType: String, val sourceTitle: String, val sourceDate: String,
    val sourceUrl: String
)

data class ProfileBullet(val topic: String, val summaryHe: String, val sourceUrl: String)
data class PartyProfile(val listId: String, val sourceIndex: String, val bullets: List<ProfileBullet>)

data class ElectionData(
    val revision: Int, val updatedAt: String, val sourceVersion: String,
    val questions: List<Question>, val lists: List<ElectionList>,
    val positions: Map<Pair<String, String>, Position>,
    val profiles: Map<String, PartyProfile> = emptyMap()
) {
    companion object {
        fun parse(raw: String): ElectionData {
            val root = JSONObject(raw)
            require(root.getInt("schemaVersion") == 1)
            require(root.getString("electionId") == "il-knesset-26")
            val snapshotHasRussian = (root.getInt("revision") == 1 && root.getString("sourceVersion") == "2026-09-30") ||
                (root.getInt("revision") == 2 && root.getString("sourceVersion") == "2026-10-03-platform-review")
            val questions = root.getJSONArray("questions").let { arr ->
                (0 until arr.length()).map { i ->
                    arr.getJSONObject(i).let { q ->
                        val id = q.getString("id")
                        val ru = q.optString("ru").takeIf { it.isNotBlank() && it != "null" } ?: if (snapshotHasRussian) RussianText.questions[id] else null
                        Question(id, q.getString("he"), q.getString("en"), q.getString("ar"), q.getString("sourceUrl"), ru)
                    }
                }
            }
            val lists = root.getJSONArray("lists").let { arr ->
                (0 until arr.length()).map { i ->
                    arr.getJSONObject(i).let { p ->
                        val id = p.getString("id")
                        val ru = p.optString("ru").takeIf { it.isNotBlank() && it != "null" } ?: if (snapshotHasRussian) RussianText.lists[id] else null
                        ElectionList(id, p.getString("he"), p.getString("en"), p.getString("ballot"), p.getString("status"), p.getString("sourceUrl"), ru)
                    }
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
            val profiles = root.optJSONArray("profiles")?.let { arr ->
                (0 until arr.length()).map { i ->
                    arr.getJSONObject(i).let { profile ->
                        val bullets = profile.getJSONArray("bullets").let { items ->
                            (0 until items.length()).map { index ->
                                items.getJSONObject(index).let { bullet -> ProfileBullet(
                                    bullet.getString("topic"), bullet.getString("summary"), bullet.getString("sourceUrl")
                                ) }
                            }
                        }
                        PartyProfile(profile.getString("listId"), profile.getString("sourceIndex"), bullets)
                    }
                }.associateBy { it.listId }
            } ?: emptyMap()
            require(questions.size in setOf(18, 50) && lists.size == 38)
            require(questions.map { it.id }.distinct().size == questions.size)
            require(lists.map { it.id }.distinct().size == 38)
            require(positions.values.all { it.value in -2..2 && it.sourceUrl.startsWith("https://") })
            val questionIds = questions.map { it.id }.toSet()
            val listIds = lists.map { it.id }.toSet()
            require(positions.values.all { it.questionId in questionIds && it.listId in listIds })
            require(profiles.keys.all { it in listIds })
            require(profiles.values.all { profile -> profile.sourceIndex.startsWith("https://") && profile.bullets.all { it.topic.isNotBlank() && it.summaryHe.isNotBlank() && it.sourceUrl.startsWith("https://") } })
            if (questions.size == 50) require(profiles.size == lists.size)
            return ElectionData(root.getInt("revision"), root.getString("updatedAt"), root.getString("sourceVersion"), questions, lists, positions, profiles)
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
            val minimumCoverage = if (valid.size > 25) 0.4 else 0.7
            val score = if (valid.size >= 8 && known >= 8 && coverage >= minimumCoverage && knownWeight > 0)
                (points / knownWeight * 100).roundToInt() else null
            MatchResult(list, score, coverage, known)
        }.sortedWith(compareBy<MatchResult> { it.score == null }.thenByDescending { it.score ?: -1 }.thenBy { it.list.he })
    }
}
