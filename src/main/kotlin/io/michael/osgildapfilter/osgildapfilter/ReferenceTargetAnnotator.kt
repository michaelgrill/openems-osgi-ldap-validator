package io.michael.osgildapfilter.osgildapfilter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.startOffset


class ReferenceTargetAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {

        if (!element.isTargetPsiLiteralExpression()) {
            return
        }

        val constant = element.value as? String ?: return

        val validator = validateLdapFilter(constant)
        val globalOffset = element.startOffset + 1 // +1 => first " of a string literal

        validator.searchElements.forEach { searchElement ->
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION) {
                name = "Key"
                textAttributes = KEY
                textRange = TextRange(
                    searchElement.keyStartIndex,
                    searchElement.equalIndex,
                ) + globalOffset
            }
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION) {
                name = "Equal"
                textAttributes = SEPARATOR
                textRange = TextRange(
                    searchElement.equalIndex,
                    searchElement.equalIndex + 1,
                ) + globalOffset
            }

            when (searchElement.value) {
                is SearchElementValue.Constant -> {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION) {
                        name = "Variable"
                        textAttributes = VALUE
                        textRange = TextRange(
                            searchElement.equalIndex + 1,
                            searchElement.valueEndIndex,
                        ) + globalOffset
                    }
                }

                is SearchElementValue.Expression -> {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION) {
                        name = "VariableTopic"
                        textAttributes = DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE
                        textRange = TextRange(
                            0,
                            searchElement.value.variableTopic.length,
                        ) + globalOffset + searchElement.valueStartIndex + searchElement.value.expressionPrefix.length
                    }
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION) {
                        name = "VariableName"
                        textAttributes = DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE
                        textRange = TextRange(
                            0,
                            searchElement.value.variableName.length,
                        ) + (globalOffset + searchElement.valueStartIndex + searchElement.value.expressionPrefix.length
                                + searchElement.value.variableTopic.length + searchElement.value.expressionSeparator.length)
                    }

                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION) {
                        name = "VariablePrefix"
                        textAttributes = DefaultLanguageHighlighterColors.BRACES
                        textRange = TextRange(
                            0,
                            searchElement.value.expressionPrefix.length,
                        ) + globalOffset + searchElement.valueStartIndex
                    }
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION) {
                        name = "VariableSuffix"
                        textAttributes = DefaultLanguageHighlighterColors.BRACES
                        textRange = TextRange(
                            0,
                            searchElement.value.expressionSuffix.length,
                        ) + (globalOffset + searchElement.valueStartIndex + searchElement.value.expressionPrefix.length
                                + searchElement.value.variableTopic.length + searchElement.value.expressionSeparator.length
                                + searchElement.value.variableName.length)
                    }
                }
            }

        }

        fun markAll(vararg chars: Char, builder: AnnotationBuilderKt.() -> Unit) {
            constant.forEachIndexed { index, ch ->
                if (ch !in chars) return@forEachIndexed

                holder.newSilentAnnotation(HighlightSeverity.INFORMATION) {
                    textAttributes = PARENTHESES
                    textRange = TextRange(0, 1) + globalOffset + index
                    apply(builder)
                }
            }
        }

        markAll('(', ')') {
            textAttributes = PARENTHESES
        }
        markAll('!', '&', '|') {
            textAttributes = OPERATOR
        }

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(DefaultLanguageHighlighterColors.INLAY_DEFAULT)
            .create()

    }

}

private val SEPARATOR: TextAttributesKey =
    createTextAttributesKey("SIMPLE_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)

private val KEY: TextAttributesKey =
    createTextAttributesKey("SIMPLE_KEY", DefaultLanguageHighlighterColors.KEYWORD)

private val OPERATOR: TextAttributesKey =
    createTextAttributesKey("SIMPLE_KEY", DefaultLanguageHighlighterColors.KEYWORD)

private val VALUE: TextAttributesKey =
    createTextAttributesKey("SIMPLE_VALUE", DefaultLanguageHighlighterColors.STRING)

private val PARENTHESES: TextAttributesKey =
    createTextAttributesKey("SIMPLE_BRACKET", DefaultLanguageHighlighterColors.PARENTHESES)
