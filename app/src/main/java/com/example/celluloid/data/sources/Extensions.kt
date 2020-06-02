package com.example.celluloid.data.sources

import kotlin.experimental.ExperimentalTypeInference

// Note - autocloseable is sequence itself, not its iterator!
// So, regretfully it is not equivalent to C# try { yield } finally {}.
class AutoCloseableSequence<T>(
    private val sequence: Sequence<T>,
    private val closeAction: () -> Unit
) {
    fun <R> use(action: (Sequence<T>) -> R): R {
        val r = sequence.let(action)
        closeAction()
        return r
    }
}

@UseExperimental(ExperimentalTypeInference::class)
fun <T : AutoCloseable, R> T.useForSequence(
    @BuilderInference sequenceBody: suspend SequenceScope<R>.(T) -> Unit
) =
    AutoCloseableSequence<R>(
        sequence { sequenceBody(this@useForSequence) },
        ::close
    )