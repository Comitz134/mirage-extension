package eu.kanade.tachiyomi.extension.all.mirage

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

class Mirage : HttpSource() {
    override val name = "Mirage"
    override val baseUrl = "https://mirage-comitz.vercel.app"
    override val lang = "all"
    override val supportsLatest = true

    // Listagem Inicial
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/api/mihon?page=$page", headers)
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/api/mihon?page=$page&order=latest", headers)
    
    override fun popularMangaParse(response: Response): MangasPage {
        val json = JSONObject(response.body!!.string())
        val items = json.getJSONArray("mangas")
        val mangas = mutableListOf<SManga>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            mangas.add(SManga.create().apply {
                url = item.getString("id")
                title = item.getString("title")
                thumbnail_url = item.getString("thumbnail")
            })
        }
        return MangasPage(mangas, json.getBoolean("hasMore"))
    }

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    // Pesquisa
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/api/mihon?q=$query&page=$page", headers)
    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    // Detalhes do Manga
    override fun mangaDetailsRequest(manga: SManga): Request = GET("$baseUrl/api/mihon/details?id=${manga.url}", headers)
    override fun mangaDetailsParse(response: Response): SManga {
        val json = JSONObject(response.body!!.string())
        return SManga.create().apply {
            title = json.getString("title")
            artist = json.getString("artist")
            author = json.getString("author")
            description = json.getString("description")
            genre = json.getJSONArray("genre").let { arr -> 
                (0 until arr.length()).joinToString { arr.getString(it) }
            }
            status = SManga.COMPLETED
            thumbnail_url = json.getString("thumbnail")
        }
    }

    // Capítulos (No Hitomi/Mirage é sempre 1 capítulo por galeria)
    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)
    override fun chapterListParse(response: Response): List<SChapter> {
        val id = JSONObject(response.body!!.string()).getString("id")
        return listOf(SChapter.create().apply {
            name = "Gallery Content"
            url = id
            chapter_number = 1f
        })
    }

    // Páginas (Imagens)
    override fun pageListRequest(chapter: SChapter): Request = GET("$baseUrl/api/mihon/details?id=${chapter.url}", headers)
    override fun pageListParse(response: Response): List<Page> {
        val json = JSONObject(response.body!!.string())
        val pagesJson = json.getJSONArray("pages")
        val pages = mutableListOf<Page>()
        for (i in 0 until pagesJson.length()) {
            pages.add(Page(i, "", pagesJson.getJSONObject(i).getString("imageUrl")))
        }
        return pages
    }

    override fun imageUrlParse(response: Response): String = ""
}
