package com.example.celluloid.ui.board

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.celluloid.R
import com.example.celluloid.data.models.Film
import com.example.celluloid.data.sources.AuthSource
import com.example.celluloid.data.sources.FilmsRepository
import java.io.File

class PosterFragment : Fragment(R.layout.fragment_poster) {
    companion object {
        const val FILM_ID = "film_id"
    }

    private val user by lazy { AuthSource(activity!!).getCurrentUser() }
    private val films by lazy { FilmsRepository(activity!!) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ) =
        super.onCreateView(inflater, container, state)?.apply {

            val film = films.get(arguments!!.getInt(FILM_ID))
            updateStarred(film)
            findViewById<ImageView>(R.id.poster_star_on)!!.setOnClickListener {
                user.stars.remove(film.id)
                updateStarred(film)
            }
            findViewById<ImageView>(R.id.poster_star_off)!!.setOnClickListener {
                user.stars.add(film.id)
                updateStarred(film)
            }
            findViewById<TextView>(R.id.poster_title)!!.text = film.title
            findViewById<TextView>(R.id.poster_genre)!!.text =
                getString(R.string.genre_format, film.genre)
            findViewById<TextView>(R.id.poster_year)!!.text =
                getString(R.string.year_format, film.year)
            findViewById<TextView>(R.id.poster_description)!!.text = film.description
            findViewById<TextView>(R.id.poster_starring)!!.text =
                getString(R.string.starring_format, film.starring)
            findViewById<ImageView>(R.id.poster_image)!!.setImageBitmap(
                BitmapFactory.decodeFile(File(activity!!.cacheDir, "${film.id}.jpg").absolutePath)
            )
        }

    private fun View.updateStarred(film: Film) {
        val starred = user.stars.contains(film.id)
        findViewById<ImageView>(R.id.poster_star_on)!!.visibility = toVisibility(starred)
        findViewById<ImageView>(R.id.poster_star_off)!!.visibility = toVisibility(!starred)
    }

    private fun toVisibility(value: Boolean) = when (value) {
        true -> View.VISIBLE
        false -> View.INVISIBLE
    }
}
