package com.example.celluloid.data.sources

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

class DbAdapter<E : Any>(
    context: Context?,
    private val kClass: KClass<E>,
    private val kFactory: KFunction<E> = kClass.primaryConstructor
        ?: throw IllegalArgumentException("No factory provided for class `${kClass.simpleName}` without primary constructor."),
    private val kTable: String = kClass.simpleName ?: "_${kClass.hashCode()}"
) : SQLiteOpenHelper(context, "$kTable.db", null, 1) {
    private val kParameters by lazy { kFactory.parameters.map { it.name }.toTypedArray() }
    private val kProperties by lazy {
        kParameters.map { name ->
            kClass.memberProperties.first { it.name == name }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val columnDeclarations =
            kFactory.parameters.joinToString(",") { it.name + " " + it.type.toSQL() }
        db.execSQL("CREATE TABLE IF NOT EXISTS $kTable ($columnDeclarations);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        TODO("not implemented")
    }

    @SuppressLint("Recycle")
    fun fetchAll(): AutoCloseableSequence<E> =
        writableDatabase.query(
            kTable, kParameters,
            null, null, null, null, null
        ).useForSequence { db ->
            val params = kParameters.map {
                val index = db.getColumnIndexOrThrow(it)
                return@map lazy { Pair(db.getType(index), index) }
            }
            while (db.moveToNext()) {
                val kFactoryParams = params.map {
                    @Suppress("IMPLICIT_CAST_TO_ANY")
                    when (it.value.first) {
                        Cursor.FIELD_TYPE_INTEGER -> db.getIntOrNull(it.value.second)
                        Cursor.FIELD_TYPE_STRING -> db.getStringOrNull(it.value.second)
                        else -> TODO("type binding not implemented")
                    }
                }
                yield(kFactory.call(*kFactoryParams.toTypedArray()))
            }
        }

    fun insert(vararg elements: E) {
        val columns = kParameters.joinToString(",")
        val bindings = elements.joinToString(",") {
            "(" + kParameters.joinToString(",") { "?" } + ")"
        }
        val values = elements.flatMap { element ->
            kProperties.map { it.get(element) }
        }
        writableDatabase.execSQL(
            "INSERT INTO $kTable ($columns) VALUES $bindings", values.toTypedArray()
        )
    }

    fun remove(vararg elements: E) {
        val bindings = elements.joinToString(" or ") {
            "(" + kParameters.joinToString(" and ") { "$it=?" } + ")"
        }
        val values = elements.flatMap { element ->
            kProperties.map { it.get(element) }
        }
        writableDatabase.execSQL(
            "DELETE FROM $kTable WHERE $bindings", values.toTypedArray()
        )
    }
}

private fun KType.toSQL() = when (jvmErasure) {
    Int::class -> "INTEGER"
    String::class -> "NVARCHAR(220)"
    else -> TODO("type binding not implemented for type $jvmErasure")
}
