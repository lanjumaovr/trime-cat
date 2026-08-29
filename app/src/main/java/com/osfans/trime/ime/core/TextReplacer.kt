/*
 * SPDX-FileCopyrightText: 2026 lanjumaovr
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.osfans.trime.ime.core

/**
 * 文本替换引擎：在上屏前对文本应用一套替换规则。
 * 当前为最小验证版本，规则硬编码；后续可扩展为订阅导入 + JS 脚本。
 */
object TextReplacer {

    private const val ENABLED = true

    /** 标点后追加的固定尾缀（依次应用） */
    private val PUNCT_SUFFIXES = listOf("喵", "🐾")

    /** 需要在其后追加尾缀的标点集合 */
    private val PUNCT_SET = "。，！？.,!?~～;；:：、"

    /** 精确替换规则（按顺序应用，每对只替换一次，避免迭代死循环） */
    private val REPLACE_RULES: List<Pair<String, String>> = listOf(
        "你" to "主人",
        "我" to "本喵",
    )

    /**
     * 对即将上屏的文本应用全部替换规则。
     */
    fun transform(text: String): String {
        if (!ENABLED || text.isEmpty()) return text
        var result = text
        for ((match, replace) in REPLACE_RULES) {
            result = result.replace(match, replace)
        }
        result = appendAfterPunct(result)
        return result
    }

    /** 在每个标点后依次追加尾缀。 */
    private fun appendAfterPunct(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            sb.append(c)
            if (c in PUNCT_SET) {
                for (suffix in PUNCT_SUFFIXES) {
                    sb.append(suffix)
                }
            }
        }
        return sb.toString()
    }
}