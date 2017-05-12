/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.injection

import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.Trinity
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import org.intellij.plugins.intelliLang.inject.InjectedLanguage
import org.intellij.plugins.intelliLang.inject.InjectorUtils
import org.intellij.plugins.intelliLang.inject.config.BaseInjection
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.*
import java.util.*

data class InjectionSplitResult(val isUnparsable: Boolean, val ranges: List<Trinity<PsiLanguageInjectionHost, InjectedLanguage, TextRange>>)

fun splitLiteralToInjectionParts(injection: BaseInjection, literal: KtStringTemplateExpression): InjectionSplitResult? {
    InjectorUtils.getLanguage(injection) ?: return null

    val unparsableRef = Ref.create(false)
    val parts = collectParts(literal, injection.prefix, injection.suffix, unparsableRef)
    if (parts.isEmpty()) return null

    val result = ArrayList<Trinity<PsiLanguageInjectionHost, InjectedLanguage, TextRange>>()
    val len = parts.size

    var i = 0
    while (i < len) {
        var curPrefix: String? = null
        var o = parts[i]
        if (o is String) {
            curPrefix = o
            if (i == len - 1) return null
            o = parts[++i]
        }

        var curSuffix: String? = null
        var curHost: KtStringTemplateEntry? = null
        if (o is KtLiteralStringTemplateEntry || o is KtEscapeStringTemplateEntry) {
            curHost = o as KtStringTemplateEntry
            if (i == len - 2) {
                val next = parts[i + 1]
                if (next is String) {
                    i++
                    curSuffix = next
                }
            }
        }
        if (curHost == null) {
            unparsableRef.set(java.lang.Boolean.TRUE)
        }
        else {
            if (curHost !is KtLiteralStringTemplateEntry && curHost !is KtEscapeStringTemplateEntry) {
                val textRange = ElementManipulators.getManipulator(curHost).getRangeInElement(curHost)
                TextRange.assertProperRange(textRange, injection)
                val injectedLanguage = InjectedLanguage.create(injection.injectedLanguageId, curPrefix, curSuffix, true)!!
                result.add(Trinity.create(literal, injectedLanguage, textRange))
            }
            else {
                val textRange = TextRange.from(curHost.startOffsetInParent, curHost.textLength)
                TextRange.assertProperRange(textRange, injection)
                val injectedLanguage = InjectedLanguage.create(injection.injectedLanguageId, curPrefix, curSuffix, true)!!
                result.add(Trinity.create(literal, injectedLanguage, textRange))
            }
        }
        i++
    }

    return InjectionSplitResult(unparsableRef.get(), result)
}

private fun collectParts(literal: KtStringTemplateExpression, prefix: String, suffix: String, unparsable: Ref<Boolean>): List<Any> {
    val result = ArrayList<Any>()
    addStringFragment(prefix, result)
    collectParts(literal, result, unparsable)
    addStringFragment(suffix, result)
    return result
}

private val NO_VALUE_NAME = "missingValue"

private fun collectParts(literal: KtStringTemplateExpression, result: MutableList<Any>, unparsable: Ref<Boolean>) {
    val children = literal.children
    for (child in children) {
        when (child.node.elementType) {
            KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY, KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY -> {
                result.add(child)
            }

            KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY -> {
                val valueName = (child as KtSimpleNameStringTemplateEntry).expression?.text ?: NO_VALUE_NAME
                addStringFragment(valueName, result)
            }
            KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY -> {
                unparsable.set(java.lang.Boolean.TRUE)
                addStringFragment(NO_VALUE_NAME, result)
            }
            else -> {
                unparsable.set(java.lang.Boolean.TRUE)
                result.add(child)
            }
        }
    }
}

private fun addStringFragment(string: String, result: MutableList<Any>) {
    if (StringUtil.isEmpty(string)) return
    val size = result.size
    val last = if (size > 0) result[size - 1] else null
    if (last is String) {
        result[size - 1] = last.toString() + string
    }
    else {
        result.add(string)
    }
}
