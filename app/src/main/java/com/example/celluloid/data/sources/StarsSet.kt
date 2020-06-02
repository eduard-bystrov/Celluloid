package com.example.celluloid.data.sources

import android.content.Context

class StarsSet(context: Context, private val username: String) : MutableSet<Int> {
    data class Star(val username: String, val filmId: Int)

    private val setImpl = mutableSetOf<Int>()
    private val db = DbAdapter(context, Star::class)

    init {
        db.fetchAll().use { entries -> setImpl.addAll(entries.map { it.filmId }) }
    }

    override val size = setImpl.size

    override fun contains(element: Int) = setImpl.contains(element)

    override fun containsAll(elements: Collection<Int>) = setImpl.containsAll(elements)

    override fun isEmpty() = setImpl.isEmpty()

    override fun add(element: Int): Boolean {
        if (setImpl.add(element)) {
            db.insert(Star(username, element))
            return true
        }
        return false
    }

    override fun remove(element: Int): Boolean {
        if (setImpl.remove(element)) {
            db.remove(Star(username, element))
            return true
        }
        return false
    }

    override fun addAll(elements: Collection<Int>): Boolean {
        val addedElements = elements.filter { setImpl.add(it) }
        if (addedElements.any()) {
            db.insert(*addedElements.map { Star(username, it) }.toTypedArray())
            return true
        }
        return false
    }

    override fun removeAll(elements: Collection<Int>): Boolean {
        val removedElements = elements.filter { setImpl.remove(it) }
        if (removedElements.any()) {
            db.remove(*removedElements.map { Star(username, it) }.toTypedArray())
            return true
        }
        return false
    }

    override fun retainAll(elements: Collection<Int>): Boolean {
        val removeElements = setImpl.minus(elements)
        if (removeElements.any()) {
            setImpl.retainAll(elements)
            db.remove(*removeElements.map { Star(username, it) }.toTypedArray())
            return true
        }
        return false
    }

    override fun clear() {
        db.remove(*setImpl.map { Star(username, it) }.toTypedArray())
        setImpl.clear()
    }

    override fun iterator() = object : MutableIterator<Int> {
        private val iteratorImpl = setImpl.iterator()
        private var current: Int = 0

        override fun hasNext() = iteratorImpl.hasNext()

        override fun next(): Int {
            current = iteratorImpl.next()
            return current
        }

        override fun remove() {
            db.remove(Star(username, current))
            iteratorImpl.remove()
        }
    }
}