package com.amgm.notebook.ui

/** Compose UI テスト用の testTag 定数。`*_PREFIX` は末尾に識別子を連結して使う。 */
object TestTags {
    // ノート一覧
    const val NOTE_LIST_FAB = "note_list_fab"
    const val NOTE_LIST_SEARCH = "note_list_search"
    const val NOTE_LIST_TAGS_BUTTON = "note_list_tags_button"
    const val NOTE_LIST_ITEM_PREFIX = "note_list_item_"
    const val NOTE_LIST_FILTER_CHIP_PREFIX = "note_list_filter_chip_"
    const val NOTE_LIST_DELETE_PREFIX = "note_list_delete_"
    const val NOTE_LIST_DELETE_CONFIRM = "note_list_delete_confirm"
    const val NOTE_LIST_EMPTY = "note_list_empty"

    // ノート編集
    const val NOTE_EDIT_TITLE = "note_edit_title"
    const val NOTE_EDIT_BODY = "note_edit_body"
    const val NOTE_EDIT_MODE_EDIT = "note_edit_mode_edit"
    const val NOTE_EDIT_MODE_PREVIEW = "note_edit_mode_preview"
    const val NOTE_EDIT_PREVIEW = "note_edit_preview"
    const val NOTE_EDIT_TAG_CHIP_PREFIX = "note_edit_tag_chip_"
    const val NOTE_EDIT_PROPERTY_PREFIX = "note_edit_property_"
    const val NOTE_EDIT_PROPERTY_REMOVE_PREFIX = "note_edit_property_remove_"
    const val NOTE_EDIT_ADD_PROPERTY = "note_edit_add_property"
    const val NOTE_EDIT_ADD_PROPERTY_NAME = "note_edit_add_property_name"
    const val NOTE_EDIT_ADD_PROPERTY_TYPE_PREFIX = "note_edit_add_property_type_"
    const val NOTE_EDIT_ADD_PROPERTY_CONFIRM = "note_edit_add_property_confirm"
    const val NOTE_EDIT_TABLE_BUTTON_PREFIX = "note_edit_table_button_"
    const val NOTE_EDIT_SAVE = "note_edit_save"
    const val NOTE_EDIT_BACK = "note_edit_back"
    const val NOTE_EDIT_DELETE = "note_edit_delete"
    const val NOTE_EDIT_DISCARD_CONFIRM = "note_edit_discard_confirm"
    const val NOTE_EDIT_DISCARD_DISMISS = "note_edit_discard_dismiss"

    // ノート編集 (v2)
    /** タグ選択チップ付近の「タグを作成」ボタン。 */
    const val NOTE_EDIT_CREATE_TAG = "note_edit_create_tag"
    /** 「タイトル変更時に最初の見出し(h1)も更新する」スイッチ。 */
    const val NOTE_EDIT_AUTO_SYNC_H1 = "note_edit_auto_sync_h1"
    /** NoteLink 行の「ノートを選ぶ」ボタン。末尾に `PropertyName.value`。 */
    const val NOTE_EDIT_LINK_PICK_PREFIX = "note_edit_link_pick_"
    /** NoteLink 行の選択済みノートチップ。末尾に `<PropertyName.value>_<NoteId.value>`。 */
    const val NOTE_EDIT_LINK_CHIP_PREFIX = "note_edit_link_chip_"
    /** 選択済みチップの × ボタン。末尾に `<PropertyName.value>_<NoteId.value>`。 */
    const val NOTE_EDIT_LINK_REMOVE_PREFIX = "note_edit_link_remove_"

    // ノートピッカー (v2)
    const val NOTE_PICKER = "note_picker"
    const val NOTE_PICKER_SEARCH = "note_picker_search"
    /** 絞り込みチップ「すべて」。 */
    const val NOTE_PICKER_FILTER_ALL = "note_picker_filter_all"
    /** 絞り込みタグチップ。末尾に `TagId.value`。 */
    const val NOTE_PICKER_FILTER_TAG_PREFIX = "note_picker_filter_tag_"
    /** 候補ノート行（チェックボックス行全体）。末尾に `NoteId.value`。 */
    const val NOTE_PICKER_ITEM_PREFIX = "note_picker_item_"
    const val NOTE_PICKER_CONFIRM = "note_picker_confirm"
    const val NOTE_PICKER_CANCEL = "note_picker_cancel"
    const val NOTE_PICKER_EMPTY = "note_picker_empty"

    // タグ一覧
    const val TAG_LIST_FAB = "tag_list_fab"
    const val TAG_LIST_ITEM_PREFIX = "tag_list_item_"
    const val TAG_LIST_EDIT_PREFIX = "tag_list_edit_"
    const val TAG_LIST_DELETE_PREFIX = "tag_list_delete_"
    const val TAG_LIST_DELETE_CONFIRM = "tag_list_delete_confirm"
    const val TAG_LIST_BACK = "tag_list_back"
    /** 複数親タグの2回目以降の出現行。末尾に `TagId.value`（初出行は `TAG_LIST_ITEM_PREFIX`）。 */
    const val TAG_LIST_ITEM_REPEAT_PREFIX = "tag_list_repeat_item_"

    // タグ編集
    const val TAG_EDIT_NAME = "tag_edit_name"
    const val TAG_EDIT_PARENT_CHIP_PREFIX = "tag_edit_parent_chip_"
    const val TAG_EDIT_ADD_DEFINITION = "tag_edit_add_definition"
    const val TAG_EDIT_DEFINITION_NAME_PREFIX = "tag_edit_definition_name_"
    /** 末尾は `<index>_<Text|Number|Boolean|Date>`。 */
    const val TAG_EDIT_DEFINITION_TYPE_PREFIX = "tag_edit_definition_type_"
    const val TAG_EDIT_DEFINITION_REQUIRED_PREFIX = "tag_edit_definition_required_"
    const val TAG_EDIT_DEFINITION_REMOVE_PREFIX = "tag_edit_definition_remove_"
    /** NoteLink 定義の既定絞り込みタグ。末尾は `<index>_<TagId.value|none>`。 */
    const val TAG_EDIT_DEFINITION_LINK_TARGET_PREFIX = "tag_edit_definition_link_target_"
    const val TAG_EDIT_SAVE = "tag_edit_save"
    const val TAG_EDIT_BACK = "tag_edit_back"

    // タグ別表ビュー
    const val TAG_TABLE = "tag_table"
    const val TAG_TABLE_HEADER_PREFIX = "tag_table_header_"
    const val TAG_TABLE_ROW_PREFIX = "tag_table_row_"
    const val TAG_TABLE_BACK = "tag_table_back"
    const val TAG_TABLE_EDIT = "tag_table_edit"
    const val TAG_TABLE_EMPTY = "tag_table_empty"
    /** 表ビューの「ノートを追加」FAB。 */
    const val TAG_TABLE_ADD_NOTE = "tag_table_add_note"

    // 表タブ (v3)
    const val TABLE_INDEX_LIST = "table_index_list"
    const val TABLE_INDEX_EMPTY = "table_index_empty"
    /** 表タブの行。末尾に `TagId.value`。 */
    const val TABLE_INDEX_ITEM_PREFIX = "table_index_item_"

    // ボトムナビゲーション (v3)
    const val BOTTOM_NAV = "bottom_nav"
    const val BOTTOM_NAV_NOTES = "bottom_nav_notes"
    const val BOTTOM_NAV_TAGS = "bottom_nav_tags"
    const val BOTTOM_NAV_TABLES = "bottom_nav_tables"
}
