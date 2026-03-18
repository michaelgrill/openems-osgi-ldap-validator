package io.michael.osgildapfilter.osgildapfilter

import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.childrenOfType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun PsiElement.isTargetPsiLiteralExpression(): Boolean {
    contract {
        returns(true) implies (this@isTargetPsiLiteralExpression is PsiLiteralExpression)
    }

    if (this !is PsiLiteralExpression) return false

    val parent = this.parent as? PsiNameValuePair ?: return false
    return parent.children.any { it is PsiIdentifier && it.text == "target" }
}

fun PsiClass.getDesignateOcdClass(): PsiClass? {
    val clazzAnnotations = this.childrenOfType<PsiModifierList>()
        .flatMap { it.childrenOfType<PsiAnnotation>() }

    val designate = clazzAnnotations.find { it.qualifiedName == DESIGNATE_CLASS_NAME }
        ?: return null

    val psiJavaCodeReferenceElement = designate.parameterList.childrenOfType<PsiNameValuePair>()
        .filter { it.attributeName == "ocd" }
        .flatMap { it.childrenOfType<PsiClassObjectAccessExpression>() }
        .map { it.firstChild }
        .firstNotNullOfOrNull { it.firstChild as? PsiJavaCodeReferenceElement } ?: return null

    val ocdClassName = psiJavaCodeReferenceElement.qualifiedName
        ?: return null

    return JavaPsiFacade.getInstance(this.project)
        .findClass(ocdClassName, GlobalSearchScope.allScope(this.project))
}