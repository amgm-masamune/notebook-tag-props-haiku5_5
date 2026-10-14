package com.amgm.notebook.ui.common

enum class UiMessage {
    Saved,
    Deleted,
    SaveBlockedByErrors,
    CyclicInheritance,
    DuplicateTagName,
    TagNotFound,
    NameRequired;

    /** スナックバー等に表示する文言。 */
    fun displayText(): String = when (this) {
        Saved -> "保存しました"
        Deleted -> "削除しました"
        SaveBlockedByErrors -> "型の不一致があるため保存できません。入力を修正してください"
        CyclicInheritance -> "タグの継承が循環するため保存できません"
        DuplicateTagName -> "同じ名前のタグが既に存在します"
        TagNotFound -> "タグが見つかりません"
        NameRequired -> "プロパティ名を入力してください（既存の名前とは重複できません）"
    }
}
