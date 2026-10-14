package com.amgm.notebook.testutil

import com.amgm.notebook.domain.repository.NoteRepository
import com.amgm.notebook.domain.repository.NoteRepositoryContract
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.repository.TagRepositoryContract
import com.amgm.notebook.testing.InMemoryRepositories

private fun createInMemoryPair(): Pair<NoteRepository, TagRepository> {
    val repositories = InMemoryRepositories()
    return repositories.notes to repositories.tags
}

/** InMemoryTagRepository が TagRepository 契約を満たすことを検証する。 */
class InMemoryTagRepositoryContractTest : TagRepositoryContract() {
    override fun createRepositories(): Pair<NoteRepository, TagRepository> = createInMemoryPair()
}

/** InMemoryNoteRepository が NoteRepository 契約を満たすことを検証する。 */
class InMemoryNoteRepositoryContractTest : NoteRepositoryContract() {
    override fun createRepositories(): Pair<NoteRepository, TagRepository> = createInMemoryPair()
}
