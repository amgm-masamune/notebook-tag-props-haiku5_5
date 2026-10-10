package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue

enum class Severity { ERROR, WARNING }

/** 検証で見つかった問題。 */
sealed interface ValidationIssue {
    val severity: Severity

    /** 型不一致（ERROR: 保存不可）。 */
    data class TypeMismatch(
        val name: PropertyName,
        val expected: PropertyType,
        val actual: PropertyValue
    ) : ValidationIssue {
        override val severity: Severity get() = Severity.ERROR
    }

    /** 必須未入力（WARNING: 保存可）。 */
    data class RequiredMissing(val name: PropertyName) : ValidationIssue {
        override val severity: Severity get() = Severity.WARNING
    }

    /** 継承による定義衝突（WARNING）。 */
    data class Conflict(val conflict: PropertyConflict) : ValidationIssue {
        override val severity: Severity get() = Severity.WARNING
    }
}

data class ValidationResult(val issues: List<ValidationIssue>) {
    val errors: List<ValidationIssue> get() = issues.filter { it.severity == Severity.ERROR }
    val warnings: List<ValidationIssue> get() = issues.filter { it.severity == Severity.WARNING }
    val hasErrors: Boolean get() = errors.isNotEmpty()
}

/**
 * ノートを実効スキーマに照らして検証する純粋ロジック。
 * `NoteValidator().validate(...)` でも `NoteValidator.validate(...)` でも呼べる。
 */
class NoteValidator {

    fun validate(note: Note, schema: EffectiveSchema): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        for (effective in schema.definitions) {
            val definition = effective.definition
            val value = note.properties[definition.name]
            val isBlank = value == null || value.isUnset
            if (!isBlank && value != null && !value.matches(definition.type)) {
                issues.add(ValidationIssue.TypeMismatch(definition.name, definition.type, value))
            } else if (isBlank && definition.required) {
                issues.add(ValidationIssue.RequiredMissing(definition.name))
            }
        }
        for (conflict in schema.conflicts) {
            issues.add(ValidationIssue.Conflict(conflict))
        }
        return ValidationResult(issues)
    }

    companion object {
        fun validate(note: Note, schema: EffectiveSchema): ValidationResult =
            NoteValidator().validate(note, schema)
    }
}
