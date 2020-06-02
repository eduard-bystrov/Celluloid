package com.example.celluloid.data.sources

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.celluloid.data.models.Film
import org.jsoup.Jsoup
import java.io.File
import java.lang.Exception
import java.lang.Integer.max
import java.net.URL

class FilmsRepository(private val activity: Activity) {
    private val db = DbAdapter(activity, Film::class)
    private val items = db.fetchAll().use { rows ->
        rows.associateBy { it.id }.toMutableMap()
    }

    fun get(id: Int) = items[id]
        ?: throw NoSuchElementException()

    fun list(atLeast: Int = 0) = sequence {
        yieldAll(items.values)
        val fetchMore = max(0, atLeast - items.count())
        yieldAll(fetch().take(fetchMore).filterNotNull())
    }

    fun fetch() = sequence {

        var cooldown = 0
        for (i in generateSequence(1) { it + 1 }) {
            try {
                val soup = Jsoup.connect("http://kino-baza.online/perevody-andreja-gavrilova/page/$i/").get()
                for (item in soup.select("div.thumb").toTypedArray()) {
                    try {
                        val href = item.select(".th-in > a").attr("href")

                        val id = href
                            .substring("http://kino-baza.online/".length).substringBefore('-').toInt()
                        val img = item.select("img").attr("src")

                        var imgUrl : URL = URL(img)

                        if (img.startsWith("/"))
                        {
                            imgUrl = URL("http://kino-baza.online$img")
                        }

                        File(activity.cacheDir, "$id.jpg").apply {
                            createNewFile()
                            writeBytes(imgUrl.readBytes())
                        }

                        var childS = Jsoup.connect(href).get()

                        val labels = childS.select("ul#finfo.finfo > noindex > li")
                            .eachText()
                            .associate {
                                it.substringBefore(':').trim() to it.substringAfter(':').trim()
                            }
                        val film = Film(
                            id,
                            item.select(".th-in > div.th-desc > a").text(),
                            labels.getOrDefault("Жанр", ""),
                            labels.getOrDefault("Год", ""),
                            childS.select("div#fdesc.fdesc.full-text.clearfix").text(),
                            labels.getOrDefault("В ролях", "")
                        )
                        if (!items.containsKey(film.id)) {
                            items[film.id] = film
                            db.insert(film)
                            yield(film)
                        }
                    } catch (e: Exception) {
                        Log.e("kinobaza", "Unable to parse film card", e)
                        Log.d("kinobaza", "HTML was ${item.html()}")
                    }
                }
                cooldown = 0
            } catch (e: Exception) {
                Log.e("kinobaza", "No internet?", e)
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        e.localizedMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
                yield(null)
                Thread.sleep(++cooldown * 500L)
            }
        }
    }
}