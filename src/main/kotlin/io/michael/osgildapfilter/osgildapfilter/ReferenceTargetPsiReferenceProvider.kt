package io.michael.osgildapfilter.osgildapfilter

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext


class ReferenceTargetPsiReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLiteralExpression::class.java),
            ReferenceTargetPsiReferenceProvider
        )
    }
}

object ReferenceTargetPsiReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {

        if (!element.isTargetPsiLiteralExpression()) {
            return emptyArray()
        }
        val constant = element.value as? String ?: return emptyArray()

        val globalOffset = 1

        val validator = validateLdapFilter(constant)

        return validator.searchElements
            .flatMap { searchElement ->
                if (searchElement.value !is SearchElementValue.Expression) {
                    return@flatMap emptyList()
                }
                if (searchElement.value.variableTopic != EXPRESSION_CONFIG_TOPIC) {
                    return@flatMap emptyList()
                }

                listOf(
                    OcdClassReference(
                        element,
                        TextRange(0, searchElement.value.variableTopic.length)
                                + (globalOffset + searchElement.valueStartIndex + searchElement.value.expressionPrefix.length)
                    ),
                    OcdClassMemberReference(
                        element,
                        TextRange(0, searchElement.value.variableName.length)
                                + (globalOffset + searchElement.valueStartIndex + searchElement.value.expressionPrefix.length
                                + searchElement.value.variableTopic.length + searchElement.value.expressionSeparator.length),
                        searchElement.value.variableName,
                    ),
                )
            }
            .toTypedArray()
    }
}

private class OcdClassReference(
    element: PsiLiteralExpression,
    range: TextRange,
) : PsiReferenceBase<PsiLiteralExpression>(element, range) {

    override fun resolve(): PsiElement? {
        return element.findParentOfType<PsiClass>()
            ?.getDesignateOcdClass()
    }

}

private class OcdClassMemberReference(
    element: PsiLiteralExpression,
    range: TextRange,
    private val member: String,
) : PsiReferenceBase<PsiLiteralExpression>(element, range) {

    override fun resolve(): PsiElement? {
        return element.findParentOfType<PsiClass>()
            ?.getDesignateOcdClass()
            ?.childrenOfType<PsiAnnotationMethod>()
            ?.find { it.name == member }
    }

}