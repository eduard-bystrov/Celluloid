package com.example.celluloid.ui.board

import com.example.celluloid.R
import com.example.celluloid.data.sources.AuthSource
import com.example.celluloid.data.sources.FilmsRepository

class StarredFragment : FilmsFragment(R.layout.fragment_starred) {
    override fun createAdapter() =
        FilmCardAdapter(
            this,
            user.stars.map(films::get).toMutableList(),
            iterator { }
        )
}