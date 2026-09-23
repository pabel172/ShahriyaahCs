package com.shahriyaahcs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.ExtractorLink
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URLEncoder

class CircleFTPProvider : MainAPI() {
    override var mainUrl = "http://new.circleftp.net"
    override var name = "CircleFTP"
    override var lang = "en"

    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val apiBase = "http://new.circleftp.net:5000/api"
    private val mapper = jacksonObjectMapper()

    private data class Post(
        val id: Any? = null,
        val name: String? = null,
        val title: String? = null,
        val type: String? = null,
        val year: Any? = null,
        val quality: String? = null,
        val poster: String? = null,
        val posterUrl: String? = null
    )

    private data class PostsResponse(
        val posts: List<Post> = emptyList()
    )

    private data class SeasonEntry(
        val seasonName: String? = null,
        val episodes: List<EpisodeEntry> = emptyList()
    )

    private data class EpisodeEntry(
        val title: String? = null,
        val link: String? = null,
        val url: String? = null
    )

    private data class PostDetail(
        val id: Any? = null,
        val name: String? = null,
        val title: String? = null,
        val year: Any? = null,
        val quality: String? = null,
        val content: Any? = null,
        val poster: String? = null,
        val posterUrl: String? = null
    )

    private data class PlaybackData(
        val url: String,
        val quality: Int = Qualities.Unknown.value,
        val referer: String = "http://new.circleftp.net"
    )

    override suspend fun search(query: String, page: Int): List<SearchResponse> {
        if (query.isBlank()) return emptyList()

        val encoded = URLEncoder.encode(query, "UTF-8")
        val response = runCatching {
            app.get("$apiBase/posts?searchTerm=$encoded&order=desc", timeout = 15000).text
        }.getOrElse { return emptyList() }

        val posts = runCatching {
            mapper.readValue<PostsResponse>(response).posts
        }.getOrElse { return emptyList() }

        return posts
            .distinctBy { it.id?.toString() ?: "${it.name}:${it.type}" }
            .take(40)
            .mapNotNull { post ->
                val id = post.id?.toString() ?: return@mapNotNull null
                val title = post.name ?: post.title ?: return@mapNotNull null
                val itemUrl = "$mainUrl/post/$id"

                when (post.type?.lowercase()) {
                    "singlevideo" -> newMovieSearchResponse(title, itemUrl, TvType.Movie) {
                        posterUrl = post.poster ?: post.posterUrl
                    }
                    "series" -> newTvSeriesSearchResponse(title, itemUrl) {
                        posterUrl = post.poster ?: post.posterUrl
                    }
                    else -> null
                }
            }
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfterLast("/post/").trim()
        if (id.isBlank()) throw ErrorLoadingException("Invalid CircleFTP post URL")

        val json = runCatching {
            app.get("$apiBase/posts/$id", timeout = 15000).text
        }.getOrElse {
            throw ErrorLoadingException("CircleFTP request failed")
        }

        val detail = runCatching {
            mapper.readValue<PostDetail>(json)
        }.getOrElse {
            throw ErrorLoadingException("Invalid CircleFTP API response")
        }

        val title = detail.name ?: detail.title ?: "CircleFTP"
        val year = detail.year?.toString()?.take(4)?.toIntOrNull()

        return if (detail.content is String) {
            newMovieLoadResponse(
                title,
                url,
                TvType.Movie,
                mapper.writeValueAsString(
                    PlaybackData(
                        url = detail.content,
                        quality = qualityFromText("${detail.quality} ${detail.content}")
                    )
                )
            ) {
                posterUrl = detail.poster ?: detail.posterUrl
                this.year = year
            }
        } else {
            val seasons = runCatching {
                mapper.convertValue(
                    detail.content,
                    mapper.typeFactory.constructCollectionType(
                        List::class.java,
                        SeasonEntry::class.java
                    )
                )
            }.getOrElse { emptyList() }

            val episodes = seasons.flatMap { seasonEntry ->
                val seasonNumber = extractSeasonNumber(seasonEntry.seasonName)

                seasonEntry.episodes.mapNotNull { episodeEntry ->
                    val streamUrl = episodeEntry.link ?: episodeEntry.url
                        ?: return@mapNotNull null

                    newEpisode(
                        mapper.writeValueAsString(
                            PlaybackData(
                                url = streamUrl,
                                quality = qualityFromText("${detail.quality} $streamUrl")
                            )
                        )
                    ) {
                        name = episodeEntry.title ?: "Episode"
                        season = seasonNumber
                        episode = extractEpisodeNumber(episodeEntry.title)
                    }
                }
            }.sortedWith(compareBy({ it.season }, { it.episode }))

            newTvSeriesLoadResponse(
                title,
                url,
                TvType.TvSeries,
                episodes
            ) {
                posterUrl = detail.poster ?: detail.posterUrl
                this.year = year
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val playback = runCatching {
            mapper.readValue<PlaybackData>(data)
        }.getOrNull() ?: return false

        callback(
            newExtractorLink(
                this.name,
                this.name,
                playback.url
            ) {
                quality = playback.quality
                referer = playback.referer
            }
        )

        return true
    }

    private fun qualityFromText(text: String): Int {
        val value = text.lowercase()
        return when {
            Regex("""\b2160p\b|\b4k\b""").containsMatchIn(value) -> Qualities.P2160.value
            Regex("""\b1440p\b""").containsMatchIn(value) -> Qualities.P1440.value
            Regex("""\b1080p\b""").containsMatchIn(value) -> Qualities.P1080.value
            Regex("""\b720p\b""").containsMatchIn(value) -> Qualities.P720.value
            Regex("""\b480p\b""").containsMatchIn(value) -> Qualities.P480.value
            Regex("""\b360p\b""").containsMatchIn(value) -> Qualities.P360.value
            else -> Qualities.Unknown.value
        }
    }

    private fun extractSeasonNumber(text: String?): Int =
        Regex("""(\d+)""").find(text ?: "")
            ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1

    private fun extractEpisodeNumber(text: String?): Int {
        val value = text ?: return 1
        val patterns = listOf(
            Regex("""E(?:pisode)?\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""\b\d+x(\d+)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d{1,3})\s+(?=720p|1080p|480p|2160p|4k)""", RegexOption.IGNORE_CASE),
            Regex("""[-–—]\s*(\d{1,3})\s*(?=[[(]|$)""")
        )

        patterns.forEach { pattern ->
            pattern.find(value)?.groupValues?.lastOrNull()?.toIntOrNull()?.let { return it }
        }
        return 1
    }
}
