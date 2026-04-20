package eu.kanade.tachiyomi.extension.all.imhentaiplus

import eu.kanade.tachiyomi.multisrc.galleryadults.GalleryAdults
import eu.kanade.tachiyomi.multisrc.galleryadults.imgAttr
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Element
import rx.Observable
import rx.schedulers.Schedulers
import java.io.IOException

class IMHentaiPlus(
    lang: String = "all",
    override val mangaLang: String = LANGUAGE_MULTI,
) : GalleryAdults(
    "IMHentaiPlus",
    "https://imhentai.xxx",
    lang = lang,
) {
    override val supportsLatest = true
    override val useIntermediateSearch: Boolean = true
    override val supportAdvancedSearch: Boolean = true
    override val supportSpeechless: Boolean = true

    class RandomCountFilter : Filter.Select<String>(
        "Random count (when Random enabled)",
        arrayOf("1", "3", "6", "9", "12"),
        0,
    )

    override fun getFilterList(): FilterList {
        val originalFilters = super.getFilterList().list.toMutableList()
        val randomIndex = originalFilters.indexOfFirst {
            it::class.simpleName == "RandomEntryFilter"
        }
        if (randomIndex >= 0) {
            originalFilters.add(randomIndex + 1, RandomCountFilter())
        } else {
            originalFilters.add(RandomCountFilter())
        }
        return FilterList(originalFilters)
    }

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<MangasPage> {
        val randomEntryFilter = filters.filterIsInstance<RandomEntryFilter>().firstOrNull()
        val randomCountFilter = filters.filterIsInstance<RandomCountFilter>().firstOrNull()
        val countMap = arrayOf(1, 3, 6, 9, 12)
        val count = countMap[randomCountFilter?.state ?: 0]

        return when {
            randomEntryFilter?.state == true -> {
                Observable.fromCallable {
                    val results = mutableListOf<SManga>()
                    repeat(count) { index ->
                        if (index > 0) Thread.sleep(1000)
                        val response = client.newCall(randomEntryRequest()).execute()
                        results.addAll(randomEntryParse(response).mangas)
                    }
                    MangasPage(results, false)
                }.subscribeOn(Schedulers.io())
            }
            else -> super.fetchSearchManga(page, query, filters)
        }
    }

    override fun Element.mangaLang() =
        select("a:has(.thumb_flag)").attr("href")
            .removeSuffix("/")
            .substringAfterLast("/")
            .let {
                if (it == LANGUAGE_SPEECHLESS) mangaLang else it
            }

    override val client: OkHttpClient = network.cloudflareClient
        .newBuilder()
        .addInterceptor { chain ->
            val request = chain.request()
            val headers = request.headers.newBuilder()
                .removeAll("Accept-Encoding")
                .build()
            chain.proceed(request.newBuilder().headers(headers).build())
        }
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            if (!response.headers("Content-Type").toString().contains("text/html")) {
                return@addInterceptor response
            }
            val responseContentType = response.body.contentType()
            val responseString = response.body.string()
            if (responseString.contains("Overload... Please use the advanced search")) {
                response.close()
                throw IOException("IMHentai search is overloaded try again later")
            }
            response.newBuilder()
                .body(responseString.toResponseBody(responseContentType))
                .build()
        }
        .build()

    override fun Element.getInfo(tag: String): String =
        select("li:has(.tags_text:contains($tag:)) a.tag")
            .joinToString {
                val name = it.ownText()
                if (tag.contains(regexTag)) {
                    genres[name] = it.attr("href")
                        .removeSuffix("/")
                        .substringAfterLast('/')
                }
                listOf(
                    name,
                    it.select(".split_tag").text()
                        .trim()
                        .removePrefix("| "),
                )
                    .filter { s -> s.isNotBlank() }
                    .joinToString()
            }

    override fun Element.getCover() = selectFirst(".left_cover img")?.imgAttr()

    override val mangaDetailInfoSelector = ".gallery_first"

    override val thumbnailSelector = ".gthumb"

    override val pageUri = "view"
}
