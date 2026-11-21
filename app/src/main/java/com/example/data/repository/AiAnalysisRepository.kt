package com.example.data.repository

import android.util.Log
import com.example.data.model.MatchDetail
import com.example.data.model.PlayerProfile
import com.example.data.model.SportType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class TriviaQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

class AiAnalysisRepository {

    private val streamingModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val triviaSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(description = "Sports trivia question"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Four answer options"
                        ),
                        "correctIndex" to Schema.integer(description = "0-based index of correct option"),
                        "explanation" to Schema.string(description = "Brief explanation")
                    )
                )
            )
        )
    )

    private val triviaModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = triviaSchema
            }
        )
    }

    fun streamMatchAnalysis(detail: MatchDetail): Flow<String> = flow {
        val summary = detail.summary
        val statText = if (detail.stats.isNotEmpty()) {
            detail.stats.take(8).joinToString("\n") { "${it.label}: ${it.homeValue} vs ${it.awayValue}" }
        } else if (detail.cricketInnings.isNotEmpty()) {
            detail.cricketInnings.joinToString("\n") { "${it.title}: ${it.totalScore} (${it.runRate})" }
        } else {
            "Status: ${summary.clockDisplay}. ${summary.statusNote}"
        }

        val prompt = """
            You are an elite ${summary.sport.name} tactical analyst for MONTANU SCORE.
            Analyze this real match concisely in 4 sharp bullet points:
            1) Tactical Momentum & Scoreline
            2) Key Statistical Edge
            3) Crucial Matchup / Turning Point
            4) Projection
            Match: ${summary.home.name} (${summary.home.score.ifBlank { "-" }}) vs ${summary.away.name} (${summary.away.score.ifBlank { "-" }})
            League: ${summary.leagueName} | Venue: ${summary.venue} | State: ${summary.clockDisplay}
            Metrics:
            $statText
        """.trimIndent()

        val sb = StringBuilder()
        try {
            withTimeout(60_000L) {
                streamingModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        sb.append(piece)
                        emit(sb.toString())
                    }
                }
            }
            if (sb.isBlank()) {
                throw IllegalStateException("AI analysis returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Match AI analysis timed out", e)
            throw IllegalStateException("AI analysis timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Match AI analysis failed", e)
            throw e
        }
    }

    fun streamPlayerAnalysis(player: PlayerProfile): Flow<String> = flow {
        val statSummary = (player.seasonStats + player.careerStats).entries
            .joinToString(", ") { "${it.key}: ${it.value}" }

        val prompt = """
            You are a professional ${player.sport.name} scout for MONTANU SCORE.
            Write a concise 3-part scouting breakdown (Strengths, Tactical Fit, Form Outlook) for:
            Player: ${player.fullName}
            Team: ${player.currentTeam} | Role: ${player.roleOrPosition} | Nationality: ${player.nationality}
            Details: $statSummary
        """.trimIndent()

        val sb = StringBuilder()
        try {
            withTimeout(60_000L) {
                streamingModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        sb.append(piece)
                        emit(sb.toString())
                    }
                }
            }
            if (sb.isBlank()) {
                throw IllegalStateException("AI scouting returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Player AI analysis timed out", e)
            throw IllegalStateException("AI scouting timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Player AI analysis failed", e)
            throw e
        }
    }

    suspend fun generateDailyTrivia(sport: SportType): Result<List<TriviaQuestion>> {
        return try {
            val sportLabel = if (sport == SportType.FOOTBALL) "World Football (Premier League, Champions League, World Cup)" else "International Cricket (Test, ODI, T20 World Cup, IPL)"
            val prompt = "Generate 3 challenging, factually accurate multiple-choice trivia questions about $sportLabel with 4 options each."
            val response = withTimeout(60_000L) {
                triviaModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty trivia JSON from model.")
            val root = JSONObject(rawJson)
            val arr = root.getJSONArray("questions")
            val parsed = mutableListOf<TriviaQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optsArr = obj.getJSONArray("options")
                val opts = (0 until optsArr.length()).map { optsArr.getString(it) }
                if (opts.size >= 2) {
                    parsed.add(
                        TriviaQuestion(
                            question = obj.getString("question"),
                            options = opts,
                            correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, opts.lastIndex),
                            explanation = obj.optString("explanation", "")
                        )
                    )
                }
            }
            if (parsed.isEmpty()) {
                Result.failure(IllegalStateException("No trivia questions returned."))
            } else {
                Result.success(parsed)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Trivia generation timed out", e)
            Result.failure(IllegalStateException("Trivia generation timed out."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Trivia generation failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiAnalysisRepository"
    }
}
