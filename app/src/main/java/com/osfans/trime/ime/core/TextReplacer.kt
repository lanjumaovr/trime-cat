/*
 * SPDX-FileCopyrightText: 2026 lanjumaovr
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.osfans.trime.ime.core

import com.osfans.trime.data.base.DataManager
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * 文本替换引擎：在上屏前对文本应用一套替换规则。
 *
 * 规则从 [RULES_FILE]（位于用户数据目录）读取，格式为 JSON：
 * ```json
 * {
 *   "replaces": [
 *     { "match": "我", "to": "本喵" },
 *     { "match": "你", "to": "主人" }
 *   ],
 *   "suffixes": ["喵", "🐾"],
 *   "puncts": "。，！？.,!?~～;；:：、"
 * }
 * ```
 *
 * 配置文件不存在或解析失败时，回退到内置默认规则。
 */
object TextReplacer {

    private const val ENABLED = true

    /** 规则文件名，位于用户数据目录下。 */
    private const val RULES_FILE_NAME = "cat_rules.json"

    /** 默认标点尾缀（标点后依次追加）。 */
    private val DEFAULT_SUFFIXES = listOf("喵", "🐾")

    /** 默认标点集合。 */
    private val DEFAULT_PUNCTS = "。，！？.,!?~～;；:：、"

    /** 默认精确替换规则（按顺序应用）。 */
    private val DEFAULT_REPLACES = listOf(
        "你" to "主人",
        "我" to "本喵",
    )

    /** 内存中缓存的规则。 */
    private data class Rules(
        val replaces: List<Pair<String, String>>,
        val suffixes: List<String>,
        val puncts: String,
    )

    @Volatile
    private var cachedRules: Rules? = null

    private val rules: Rules
        get() {
            cachedRules?.let { return it }
            val loaded = loadRules()
            cachedRules = loaded
            return loaded
        }

    private fun rulesFile(): File = File(DataManager.userDataDir, RULES_FILE_NAME)

    /**
     * 对即将上屏的文本应用全部替换规则。
     */
    fun transform(text: String): String {
        if (!ENABLED || text.isEmpty()) return text
        val r = rules
        var result = text
        for ((match, replace) in r.replaces) {
            result = result.replace(match, replace)
        }
        result = appendAfterPunct(result, r.puncts, r.suffixes)
        return result
    }

    /**
     * 从配置文件加载规则；失败时回退到默认规则。
     */
    private fun loadRules(): Rules {
        val file = rulesFile()
        if (!file.exists()) {
            Timber.d("规则文件不存在，使用默认规则: %s", file.absolutePath)
            return defaultRules()
        }
        return try {
            val text = file.readText()
            parseRules(text)
        } catch (e: Exception) {
            Timber.w(e, "规则文件解析失败，使用默认规则")
            defaultRules()
        }
    }

    private fun parseRules(text: String): Rules {
        val obj = JSONObject(text)
        val replaces = mutableListOf<Pair<String, String>>()
        val arr: JSONArray? = obj.optJSONArray("replaces")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val match = item.optString("match", "")
                val to = item.optString("to", "")
                if (match.isNotEmpty()) {
                    replaces.add(match to to)
                }
            }
        }
        val suffixes = mutableListOf<String>()
        val suffixArr: JSONArray? = obj.optJSONArray("suffixes")
        if (suffixArr != null) {
            for (i in 0 until suffixArr.length()) {
                suffixes.add(suffixArr.optString(i, ""))
            }
        }
        val puncts = obj.optString("puncts", DEFAULT_PUNCTS)
        return Rules(
            replaces = if (replaces.isEmpty()) DEFAULT_REPLACES else replaces,
            suffixes = if (suffixes.isEmpty()) DEFAULT_SUFFIXES else suffixes.filter { it.isNotEmpty() },
            puncts = puncts.ifEmpty { DEFAULT_PUNCTS },
        )
    }

    private fun defaultRules(): Rules =
        Rules(
            replaces = DEFAULT_REPLACES,
            suffixes = DEFAULT_SUFFIXES,
            puncts = DEFAULT_PUNCTS,
        )

    /** 在每个标点后依次追加尾缀。 */
    private fun appendAfterPunct(input: String, puncts: String, suffixes: List<String>): String {
        if (suffixes.isEmpty()) return input
        val sb = StringBuilder()
        for (c in input) {
            sb.append(c)
            if (c in puncts) {
                for (suffix in suffixes) {
                    sb.append(suffix)
                }
            }
        }
        return sb.toString()
    }
}