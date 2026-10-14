package com.amgm.notebook.domain.error

import com.amgm.notebook.domain.model.TagId

/** 継承の循環（自己参照を含む）。自己参照のとき path は [tagId, tagId]。 */
class CyclicInheritanceException(
    val tagId: TagId,
    val path: List<TagId>
) : RuntimeException("タグの継承が循環します: ${path.joinToString(" -> ") { it.value }}")

/** タグ名の重複（大文字小文字を区別しない）。 */
class DuplicateTagNameException(val name: String) :
    RuntimeException("タグ名が重複しています: $name")

/** 存在しないタグIDの参照。 */
class TagNotFoundException(val tagId: TagId) :
    RuntimeException("タグが見つかりません: ${tagId.value}")
