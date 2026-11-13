package com.example.android_basic.feature.flow

import android.content.Context
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.runBlocking

fun EditText.textChanges(): Flow<String> = callbackFlow {
    val watcher = doOnTextChanged { text, _, _, _ ->
        trySend(text?.toString().orEmpty())
    }
    awaitClose {
        removeTextChangedListener(watcher)
    }
}

@OptIn(FlowPreview::class)
fun collect(context: Context) {
    val editText = EditText(context)
    runBlocking {
        editText.textChanges()
            .debounce(300)
            .distinctUntilChanged()
            .collectLatest {
                println("search, keyword=$it")
            }
    }

}