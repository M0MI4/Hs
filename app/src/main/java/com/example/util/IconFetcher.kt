package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class IconSearchResult(
    val trackName: String,
    val artistName: String,
    val artworkUrl512: String
)

object IconFetcher {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .build()
    }

    private val curatedSearchTerms = mapOf(
        "snapchat prime v6" to "Snapchat",
        "snapchat elite plus v4" to "Snapchat",
        "instagram prime" to "Instagram",
        "tiktok prime" to "TikTok",
        "telegram prime" to "Telegram Messenger",
        "messenger prime" to "Messenger",
        "facebook prime" to "Facebook",
        "x prime" to "X Twitter",
        "sendit prime" to "sendit",
        "capcut prime" to "CapCut Video Editor",
        "alight motion prime" to "Alight Motion",
        "tracket motion prime" to "Video Motion Editor",
        "remini prime" to "Remini AI Photo Enhancer",
        "photoroom prime" to "Photoroom AI Photo Editor",
        "picsart pro prime" to "Picsart AI Photo Editor",
        "lightroom prime" to "Adobe Lightroom Photo Editor",
        "inshot prime" to "InShot Video Editor",
        "kinemaster prime" to "KineMaster Video Editor",
        "videoleap prime" to "Videoleap Editor",
        "cute cut pro prime" to "Cute CUT",
        "polarr prime" to "Polarr Photo Filters",
        "lovi prime" to "Lovi Video Maker",
        "hs font emoji iphone" to "Fonts Art",
        "hs keyboard emoji iphone" to "Fonts Keyboard",
        "emoji ios 16" to "Emoji Keyboard",
        "gallery iphone prime" to "Photo Gallery Vault",
        "files iphone prime" to "File Manager Explorer",
        "calendar iphone prime" to "Calendar Planner",
        "dialer iphone prime" to "Phone Call Recorder Dialer",
        "youtube prime" to "YouTube",
        "mytv+ prime" to "IPTV Player",
        "netflix prime" to "Netflix",
        "black hole" to "Music Player Offline",
        "djay pro prime" to "djay DJ App",
        "hs notes prime" to "Apple Notes",
        "truecaller prime" to "Truecaller",
        "z translate prime" to "Google Translate",
        "hama cloner arm" to "Parallel Space Dual App",
        "god of war iii" to "God of War",
        "gta v" to "Grand Theft Auto iFruit",
        "gta san andreas kurdish" to "Grand Theft Auto San Andreas",
        "gta vice city kurdish" to "Grand Theft Auto Vice City",
        "bully kurdish" to "Bully Anniversary Edition",
        "choo choo charles" to "Choo Choo Train",
        "car parking multiplayer" to "Car Parking Multiplayer",
        "dragon 8ball pool" to "8 Ball Pool",
        "subway surfers" to "Subway Surfers",
        "minecraft" to "Minecraft",
        "truckers of europe 3" to "Truckers of Europe 3"
    )

    // Custom/Kurdish mods without an exact App Store counterpart get crisp 512x512 monogram PNGs directly
    private val customMonogramApps = setOf(
        "hs lab kurd prime",
        "baxa app"
    )

    fun buildSearchTerm(appName: String, packageName: String = ""): String {
        val key = appName.trim().lowercase()
        curatedSearchTerms[key]?.let { return it }

        val cleaned = appName
            .replace(Regex("\\b(Prime|ELITE|Plus|Pro|Kurdish|Kurd|Arm|iPhone|V\\d+|v\\d+)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleaned.isNotBlank()) return cleaned

        if (packageName.isNotBlank()) {
            val segments = packageName.split(".")
                .filter { it.isNotBlank() && it !in setOf("com", "org", "net", "app", "android", "mod", "free", "mobile") }
            if (segments.isNotEmpty()) return segments.joinToString(" ")
        }
        return appName.trim()
    }

    private fun upgradeArtworkUrlTo512(item: JSONObject): String? {
        val url512 = item.optString("artworkUrl512").trim()
        if (url512.startsWith("http")) {
            return url512
        }
        val url100 = item.optString("artworkUrl100").trim()
        if (url100.startsWith("http")) {
            return url100.replace(Regex("\\d+x\\d+bb(\\.[a-zA-Z0-9]+)?$"), "512x512bb.jpg")
        }
        val url60 = item.optString("artworkUrl60").trim()
        if (url60.startsWith("http")) {
            return url60.replace(Regex("\\d+x\\d+bb(\\.[a-zA-Z0-9]+)?$"), "512x512bb.jpg")
        }
        return null
    }

    /**
     * Parses iTunes Search API JSON and returns up to 15 512x512 icon results.
     */
    fun parseItunesSearchResults(jsonBody: String): List<IconSearchResult> {
        return try {
            val root = JSONObject(jsonBody)
            val results = root.optJSONArray("results") ?: return emptyList()
            val list = ArrayList<IconSearchResult>(results.length().coerceAtMost(15))
            for (i in 0 until results.length().coerceAtMost(15)) {
                val obj = results.optJSONObject(i) ?: continue
                val artwork512 = upgradeArtworkUrlTo512(obj) ?: continue
                val trackName = obj.optString("trackName").trim()
                val artistName = obj.optString("artistName").trim()
                list.add(
                    IconSearchResult(
                        trackName = trackName.ifBlank { "App Icon" },
                        artistName = artistName,
                        artworkUrl512 = artwork512
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("IconFetcher", "Failed to parse iTunes search results: ${e.message}")
            emptyList()
        }
    }

    /**
     * Parses iTunes Search API JSON and returns the first 512x512 icon URL.
     */
    fun extractItunesArtwork512Url(jsonBody: String): String? {
        return parseItunesSearchResults(jsonBody).firstOrNull()?.artworkUrl512
    }

    /**
     * Queries iTunes Search API for up to 15 matching software icons (512x512).
     */
    suspend fun searchIcons(query: String): List<IconSearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()
        val encodedTerm = URLEncoder.encode(cleanQuery, "UTF-8")
        val apiUrl = "https://itunes.apple.com/search?term=$encodedTerm&entity=software&limit=15"
        try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "HsStore-Android/1.3")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        return@withContext parseItunesSearchResults(body)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("IconFetcher", "searchIcons error for '$cleanQuery': ${e.message}")
        }
        emptyList()
    }

    /**
     * Builds a clean 512x512 PNG monogram URL (no emojis, crisp typography on accent background).
     */
    fun buildHighResMonogramUrl(appName: String, accentHex: String = "#007AFF"): String {
        val cleanBg = accentHex.trim().removePrefix("#").let {
            if (it.matches(Regex("^[0-9a-fA-F]{6}$"))) it else "007AFF"
        }
        val words = appName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val initials = when {
            words.size >= 2 -> "${words[0].take(1)}${words[1].take(1)}".uppercase()
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "HS"
        }
        val encoded = URLEncoder.encode(initials, "UTF-8")
        return "https://ui-avatars.com/api/?name=$encoded&size=512&background=$cleanBg&color=ffffff&bold=true&format=png"
    }

    /**
     * Fetches the highest-resolution 512x512 icon URL via iTunes Search API, falling back
     * to a clean 512x512 custom monogram PNG URL for Kurdish/custom utilities or offline cases.
     */
    suspend fun fetchHdIconUrl(
        appName: String,
        packageName: String = "",
        accentHex: String = "#007AFF"
    ): String = withContext(Dispatchers.IO) {
        val lower = appName.trim().lowercase()
        if (lower in customMonogramApps) {
            return@withContext buildHighResMonogramUrl(appName, accentHex)
        }

        val searchTerm = buildSearchTerm(appName, packageName)
        if (searchTerm.isNotBlank()) {
            val encodedTerm = URLEncoder.encode(searchTerm, "UTF-8")
            val apiUrl = "https://itunes.apple.com/search?term=$encodedTerm&entity=software&limit=1"
            try {
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "HsStore-Android/1.3")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            extractItunesArtwork512Url(body)?.let { return@withContext it }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("IconFetcher", "iTunes icon search error for '$appName': ${e.message}")
            }
        }

        buildHighResMonogramUrl(appName, accentHex)
    }
}
