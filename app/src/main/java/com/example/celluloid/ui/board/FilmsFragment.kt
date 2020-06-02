package com.example.celluloid.ui.board

import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.celluloid.R
import com.example.celluloid.data.models.Film
import com.example.celluloid.data.models.User
import com.example.celluloid.data.sources.AuthSource
import com.example.celluloid.data.sources.FilmsRepository
import java.io.File

open class FilmsFragment(layoutId: Int = R.layout.fragment_films) : Fragment(layoutId) {
    class FilmCardHolder(view: View) : RecyclerView.ViewHolder(view)
    companion object {
        const val PAGE_SIZE = 10
    }

    var waitingForNextPage = true
    protected lateinit var user: User
    protected val films by lazy { FilmsRepository(activity!!) }

    class FilmCardAdapter(
        private val fragment: FilmsFragment,
        itemsSource: Iterable<Film>,
        private val source: Iterator<Film?>
    ) : RecyclerView.Adapter<FilmCardHolder>() {
        private val items = itemsSource.toMutableList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            FilmCardHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.card_film, parent, false)
            )

        override fun onBindViewHolder(holder: FilmCardHolder, position: Int) {
            val film = items[position]
            holder.itemView.findViewById<View>(R.id.film_card).apply {
                findViewById<TextView>(R.id.film_title).text = film.title
                findViewById<TextView>(R.id.film_genre).text = film.genre
                findViewById<TextView>(R.id.film_year).text = film.year
                findViewById<ImageView>(R.id.film_image).setImageBitmap(
                    BitmapFactory.decodeFile(
                        File(
                            fragment.activity!!.cacheDir,
                            "${film.id}.jpg"
                        ).absolutePath
                    )
                )
                setOnClickListener { openFilmPoster(film) }
                setOnCreateContextMenuListener { menu, _, _ ->
                    menu.add(R.string.open_poster).setOnMenuItemClickListener {
                        openFilmPoster(film)
                        true
                    }
                    if (fragment.user.stars.contains(film.id)) {
                        menu.add(R.string.poster_star_off).setOnMenuItemClickListener {
                            fragment.user.stars.remove(film.id)
                            true
                        }
                    } else {
                        menu.add(R.string.poster_star_on).setOnMenuItemClickListener {
                            fragment.user.stars.add(film.id)
                            true
                        }
                    }
                    menu.add(android.R.string.cancel)
                }
            }
        }

        private fun openFilmPoster(film: Film) {
            fragment.findNavController().navigate(R.id.nav_poster, Bundle().apply {
                putInt(PosterFragment.FILM_ID, film.id)
            })
        }

        override fun getItemCount() =
            items.count()

        fun fetchNextPage(param: FilmsFragment) {
            val firstAddedPosition = items.count()
            val addCards =
                source.asSequence()
                    .take(PAGE_SIZE).takeWhile { it != null }
                    .filterNotNull().toList()
            if (addCards.any()) {
                items.addAll(addCards)
                param.view?.post { notifyItemRangeInserted(firstAddedPosition, addCards.size) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        user = AuthSource(activity!!).getCurrentUser()
        val filmsAdapter = createAdapter()
        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
            val upButton = findViewById<FloatingActionButton>(R.id.up)
            findViewById<RecyclerView>(R.id.grid).apply {
                upButton.setOnClickListener { smoothScrollToPosition(0) }
                adapter = filmsAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        (layoutManager as GridLayoutManager).let {
                            val pastVisibleItems = it.findFirstVisibleItemPosition()
                            upButton.visibility = when (pastVisibleItems > 1) {
                                true -> View.VISIBLE
                                false -> View.INVISIBLE
                            }
                            if (waitingForNextPage && dy > 0 &&
                                (it.childCount + pastVisibleItems) >= it.itemCount / 2
                            ) {
                                waitingForNextPage = false
                                Pager(filmsAdapter, this@FilmsFragment).execute()
                            }
                        }
                    }
                })
            }
        }
    }

    protected open fun createAdapter() =
        FilmCardAdapter(
            this,
            FirstPage(films).execute().get(),
            films.fetch().iterator()
        )

    class FirstPage(
        private val films: FilmsRepository
    ) : AsyncTask<Unit, Unit, MutableList<Film>>() {
        override fun doInBackground(vararg params: Unit?) =
            films.list(PAGE_SIZE).toMutableList()
    }

    class Pager(
        private val filmsAdapter: FilmCardAdapter,
        private val param: FilmsFragment
    ) : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            filmsAdapter.fetchNextPage(param)
            param.waitingForNextPage = true
        }
    }
}