package com.amgm.notebook.domain.service

import com.amgm.notebook.domain.tagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagTreeTest {

    private fun List<TagTreeNode>.summary() =
        map { Triple(it.tag.id.value, it.depth, it.isAdditionalParentOccurrence) }

    @Test
    fun `空なら空`() {
        assertTrue(flattenTagTree(emptyList()).isEmpty())
    }

    @Test
    fun `単純な親子は深さ優先で並ぶ`() {
        val result = flattenTagTree(listOf(tagOf("child", parents = listOf("root")), tagOf("root")))
        assertEquals(listOf(Triple("root", 0, false), Triple("child", 1, false)), result.summary())
    }

    @Test
    fun `多段のネストと兄弟の名前順`() {
        val tags = listOf(
            tagOf("r"),
            tagOf("c2", parents = listOf("r")),
            tagOf("c1", parents = listOf("r")),
            tagOf("g", parents = listOf("c1"))
        )
        assertEquals(
            listOf(
                Triple("r", 0, false),
                Triple("c1", 1, false),
                Triple("g", 2, false),
                Triple("c2", 1, false)
            ),
            flattenTagTree(tags).summary()
        )
    }

    @Test
    fun `根は大文字小文字を無視した名前順`() {
        val tags = listOf(tagOf("b", name = "banana"), tagOf("a", name = "Apple"), tagOf("c", name = "cherry"))
        assertEquals(listOf("a", "b", "c"), flattenTagTree(tags).map { it.tag.id.value })
    }

    @Test
    fun `複数親のタグは親ごとに出現し2回目以降は追加出現になる`() {
        val tags = listOf(
            tagOf("p1"),
            tagOf("p2"),
            tagOf("kid", parents = listOf("p1", "p2")),
            tagOf("grand", parents = listOf("kid"))
        )
        assertEquals(
            listOf(
                Triple("p1", 0, false),
                Triple("kid", 1, false),
                Triple("grand", 2, false),
                Triple("p2", 0, false),
                Triple("kid", 1, true),
                Triple("grand", 2, true)
            ),
            flattenTagTree(tags).summary()
        )
    }

    @Test
    fun `欠落した親は無視され他に親が無ければ根になる`() {
        val tags = listOf(tagOf("orphan", parents = listOf("ghost")), tagOf("x"))
        assertEquals(
            listOf(Triple("orphan", 0, false), Triple("x", 0, false)),
            flattenTagTree(tags).summary()
        )
    }

    @Test
    fun `欠落した親と存在する親が混在すれば存在する親の下に出る`() {
        val tags = listOf(tagOf("p"), tagOf("k", parents = listOf("ghost", "p")))
        assertEquals(listOf(Triple("p", 0, false), Triple("k", 1, false)), flattenTagTree(tags).summary())
    }

    @Test
    fun `循環データでも終了し有限個を返す`() {
        val tags = listOf(
            tagOf("root"),
            tagOf("a", parents = listOf("root", "b")),
            tagOf("b", parents = listOf("a"))
        )
        val result = flattenTagTree(tags)
        assertEquals(
            listOf(Triple("root", 0, false), Triple("a", 1, false), Triple("b", 2, false)),
            result.summary()
        )
    }

    @Test
    fun `根の無い純粋な循環と自己参照でも例外を投げない`() {
        val cycle = flattenTagTree(listOf(tagOf("a", parents = listOf("b")), tagOf("b", parents = listOf("a"))))
        assertEquals(listOf(Triple("a", 0, false), Triple("b", 1, false)), cycle.summary())
        val self = flattenTagTree(listOf(tagOf("s", parents = listOf("s"))))
        assertEquals(listOf(Triple("s", 0, false)), self.summary())
    }
}
