@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.testutil

import com.amgm.notebook.domain.util.IdGenerator
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** 任意に進められる決定的な時計。 */
class TestClock(var now: Instant = Instant.ofEpochMilli(1_000_000L)) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = now
}

/** `id-1`, `id-2`, ... を返す決定的なID採番。 */
class SequentialIdGenerator(private val prefix: String = "id-") : IdGenerator {
    private var counter = 0
    override fun newId(): String {
        counter += 1
        return "$prefix$counter"
    }
}

/** ViewModel.events を収集して時系列で返す（テスト終了時に自動で止まる）。 */
fun TestScope.recordEvents(flow: Flow<UiEvent>): MutableList<UiEvent> {
    val list = mutableListOf<UiEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        flow.collect { list.add(it) }
    }
    return list
}

/** NavController の代替。UiEvent を受けてバックスタックを更新する。 */
class FakeNavigator(start: Destination = Destination.NoteList) {
    private val stack = mutableListOf(start)
    val messages = mutableListOf<UiEvent.ShowMessage>()

    val backStack: List<Destination> get() = stack.toList()
    val current: Destination get() = stack.last()

    fun handle(event: UiEvent) {
        when (event) {
            is UiEvent.Navigate -> stack.add(event.destination)
            is UiEvent.NavigateBack -> if (stack.size > 1) stack.removeAt(stack.lastIndex)
            is UiEvent.ShowMessage -> messages.add(event)
        }
    }

    /** 溜まったイベントを全て適用し、リストを空にする。 */
    fun drain(events: MutableList<UiEvent>) {
        val copy = events.toList()
        events.clear()
        copy.forEach { handle(it) }
    }
}
