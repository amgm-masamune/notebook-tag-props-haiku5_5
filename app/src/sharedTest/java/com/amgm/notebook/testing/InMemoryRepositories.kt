package com.amgm.notebook.testing

/** 連動した [InMemoryNoteRepository] と [InMemoryTagRepository] の組。 */
class InMemoryRepositories(
    val tags: InMemoryTagRepository = InMemoryTagRepository(),
    val notes: InMemoryNoteRepository = InMemoryNoteRepository(tags)
)
