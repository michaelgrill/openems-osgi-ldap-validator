package io.michael.osgildapfilter.osgildapfilter

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.findParentOfType


class ReferenceTargetInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {

            override fun visitAnnotation(annotation: PsiAnnotation) {

                if (annotation.qualifiedName != REFERENCE_CLASS_NAME) return

                val value = annotation.findAttributeValue("target") ?: return

                val constant = JavaPsiFacade.getInstance(annotation.project)
                    .constantEvaluationHelper.computeConstantExpression(
                        value
                    ) as? String ?: return

                val globalOffset = 1
                val ldapFilter = validateLdapFilter(constant)

                ldapFilter.badCharacters.forEach { badCharacters ->
                    holder.registerProblem(
                        value,
                        TextRange(badCharacters.range.first, badCharacters.range.checkedEndExclusive) + 1,
                        badCharacters.message
                    )
                }

                val ocdClass by lazy {
                    annotation.findParentOfType<PsiClass>()
                        ?.getDesignateOcdClass()
                }

                ldapFilter.searchElements.forEach { searchElement ->
                    if (searchElement.value !is SearchElementValue.Expression) {
                        return@forEach
                    }
                    if (searchElement.value.variableTopic != EXPRESSION_CONFIG_TOPIC) {
                        return@forEach
                    }

                    val configMethod = ocdClass
                        ?.childrenOfType<PsiAnnotationMethod>()
                        ?.find { it.name == searchElement.value.variableName }

                    if (configMethod == null) {
                        holder.registerProblem(
                            value,
                            TextRange(0, searchElement.value.variableName.length)
                                    + (globalOffset + searchElement.valueStartIndex + searchElement.value.expressionPrefix.length
                                    + searchElement.value.variableTopic.length + searchElement.value.expressionSeparator.length),
                            "Config property \"${searchElement.value.variableName}\" not found"
                        )
                    }
                }
            }

        }

}