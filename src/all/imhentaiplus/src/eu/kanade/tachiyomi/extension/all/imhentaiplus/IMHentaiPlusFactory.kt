package eu.kanade.tachiyomi.extension.all.imhentaiplus

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

class IMHentaiPlusFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        IMHentaiPlus(),
        IMHentaiPlus("en", "english"),
        IMHentaiPlus("ja", "japanese"),
        IMHentaiPlus("es", "spanish"),
        IMHentaiPlus("fr", "french"),
        IMHentaiPlus("ko", "korean"),
        IMHentaiPlus("de", "german"),
        IMHentaiPlus("ru", "russian"),
        IMHentaiPlus("zh", "chinese"),
    )
}
