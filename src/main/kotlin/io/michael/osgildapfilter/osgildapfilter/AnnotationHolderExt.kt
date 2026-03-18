package io.michael.osgildapfilter.osgildapfilter

import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange

fun AnnotationHolder.newSilentAnnotation(severity: HighlightSeverity, builder: AnnotationBuilderKt.() -> Unit) {
    val builder = AnnotationBuilderKt().also(builder)

    // TODO Class not found
    //    when (val name = builder.name) {
    //        null -> this.newSilentAnnotation(severity)
    //        else -> this.newSilentAnnotationWithDebugInfo(severity, name)
    //    }

    this.newSilentAnnotation(severity)
        .also(builder::applyTo)
        .create()
}

class AnnotationBuilderKt {
    var name: String? = null
    var textAttributes: TextAttributesKey? = null
    var textRange: TextRange? = null
}

private fun AnnotationBuilderKt.applyTo(annotationBuilder: AnnotationBuilder) {
    textAttributes?.let { annotationBuilder.textAttributes(it) }
    textRange?.let { annotationBuilder.range(it) }
}

operator fun TextRange.plus(i: Int): TextRange {
    return TextRange(
        this.startOffset + i,
        this.endOffset + i,
    )
}