/*
 * Copyright 2016-2017 JetBrains s.r.o.
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

//original code from https://github.com/Kotlin/kotlinx.coroutines/blob/master/knit/src/Knit.kt
// see https://kotlinlang.org/docs/tutorials/command-line.html
//To run a script, we just pass the -script option to the compiler with the corresponding script file.
//$ kotlinc -script list_folders.kts <path_to_folder_to_inspect>
//

import Knit.Consts.API_REF_REGEX
import Knit.Consts.ARBITRARY_TIME_PREDICATE
import Knit.Consts.CLEAR_DIRECTIVE
import Knit.Consts.CODE_END
import Knit.Consts.CODE_START
import Knit.Consts.DIRECTIVE_END
import Knit.Consts.DIRECTIVE_START
import Knit.Consts.DOCS_ROOT_DIRECTIVE
import Knit.Consts.FLEXIBLE_THREAD_PREDICATE
import Knit.Consts.FLEXIBLE_TIME_PREDICATE
import Knit.Consts.INCLUDE_DIRECTIVE
import Knit.Consts.INDEX_DIRECTIVE
import Knit.Consts.KNIT_DIRECTIVE
import Knit.Consts.LINES_START_PREDICATE
import Knit.Consts.LINES_START_UNORDERED_PREDICATE
import Knit.Consts.PACKAGE_PREFIX
import Knit.Consts.SECTION_START
import Knit.Consts.SITE_ROOT_DIRECTIVE
import Knit.Consts.STARTS_WITH_PREDICATE
import Knit.Consts.TEST_DIRECTIVE
import Knit.Consts.TEST_END
import Knit.Consts.TEST_OUT_DIRECTIVE
import Knit.Consts.TEST_START
import Knit.Consts.TOC_DIRECTIVE
import java.io.*
import java.util.*
import kotlin.system.exitProcess

object Consts {
    const val DIRECTIVE_START = "<!--- "
    const val DIRECTIVE_END = "-->"

    const val TOC_DIRECTIVE = "TOC"
    const val KNIT_DIRECTIVE = "KNIT"
    const val INCLUDE_DIRECTIVE = "INCLUDE"
    const val CLEAR_DIRECTIVE = "CLEAR"
    const val TEST_DIRECTIVE = "TEST"

    const val TEST_OUT_DIRECTIVE = "TEST_OUT"

    const val SITE_ROOT_DIRECTIVE = "SITE_ROOT"
    const val DOCS_ROOT_DIRECTIVE = "DOCS_ROOT"
    const val INDEX_DIRECTIVE = "INDEX"

    const val CODE_START = "```kotlin"
    const val CODE_END = "```"

    const val TEST_START = "```text"
    const val TEST_END = "```"

    const val SECTION_START = "##"

    const val PACKAGE_PREFIX = "package "
    const val STARTS_WITH_PREDICATE = "STARTS_WITH"
    const val ARBITRARY_TIME_PREDICATE = "ARBITRARY_TIME"
    const val FLEXIBLE_TIME_PREDICATE = "FLEXIBLE_TIME"
    const val FLEXIBLE_THREAD_PREDICATE = "FLEXIBLE_THREAD"
    const val LINES_START_UNORDERED_PREDICATE = "LINES_START_UNORDERED"
    const val LINES_START_PREDICATE = "LINES_START"

    val API_REF_REGEX = Regex("(^|[ \\]])\\[([A-Za-z0-9_.]+)\\]($|[^\\[\\(])")
}

/**
 * Here is start the script code
 */
if (args.isEmpty()) {
    println("Usage: Knit <markdown-files>")
    exitProcess(0)
}
args.forEach { knit(it) }

fun knit(markdownFileName: String) {
    println("*** Reading $markdownFileName")
    val markdownFile = File(markdownFileName)
    val tocLines = arrayListOf<String>()
    var knitRegex: Regex? = null
    val includes = arrayListOf<Include>()
    val codeLines = arrayListOf<String>()
    val testLines = arrayListOf<String>()
    var testOut: String? = null
    val testOutLines = arrayListOf<String>()
    var lastPgk: String? = null
    val files = mutableSetOf<File>()
    val allApiRefs = arrayListOf<ApiRef>()
    val remainingApiRefNames = mutableSetOf<String>()
    var siteRoot: String? = null
    var docsRoot: String? = null
    // read markdown file
    var putBackLine: String? = null
    val markdown = markdownFile.withMarkdownTextReader {
        mainLoop@ while (true) {
            val inLine = putBackLine ?: readLine() ?: break
            putBackLine = null
            val directive = directive(inLine)
            if (directive != null && markdownPart == MarkdownPart.TOC) {
                markdownPart = MarkdownPart.POST_TOC
                postTocText += inLine
            }
            when (directive?.name) {
                TOC_DIRECTIVE -> {
                    requireSingleLine(directive)
                    require(directive.param.isEmpty()) { "$TOC_DIRECTIVE directive must not have parameters" }
                    require(markdownPart == MarkdownPart.PRE_TOC) { "Only one TOC directive is supported" }
                    markdownPart = MarkdownPart.TOC
                }
                KNIT_DIRECTIVE -> {
                    requireSingleLine(directive)
                    require(!directive.param.isEmpty()) { "$KNIT_DIRECTIVE directive must include regex parameter" }
                    require(knitRegex == null) { "Only one KNIT directive is supported" }
                    knitRegex = Regex("\\((" + directive.param + ")\\)")
                    continue@mainLoop
                }
                INCLUDE_DIRECTIVE -> {
                    if (directive.param.isEmpty()) {
                        require(!directive.singleLine) { "$INCLUDE_DIRECTIVE directive without parameters must not be single line" }
                        readUntilTo(DIRECTIVE_END, codeLines)
                    } else {
                        val include = Include(Regex(directive.param))
                        if (directive.singleLine) {
                            include.lines += codeLines
                            codeLines.clear()
                        } else {
                            readUntilTo(DIRECTIVE_END, include.lines)
                        }
                        includes += include
                    }
                    continue@mainLoop
                }
                CLEAR_DIRECTIVE -> {
                    requireSingleLine(directive)
                    require(directive.param.isEmpty()) { "$CLEAR_DIRECTIVE directive must not have parameters" }
                    codeLines.clear()
                    continue@mainLoop
                }
                TEST_OUT_DIRECTIVE -> {
                    require(!directive.param.isEmpty()) { "$TEST_OUT_DIRECTIVE directive must include file name parameter" }
                    flushTestOut(markdownFile.parentFile, testOut, testOutLines)
                    testOut = directive.param
                    readUntil(DIRECTIVE_END).forEach { testOutLines += it }
                }
                TEST_DIRECTIVE -> {
                    require(lastPgk != null) { "'$PACKAGE_PREFIX' prefix was not found in emitted code" }
                    require(testOut != null) { "$TEST_OUT_DIRECTIVE directive was not specified" }
                    var predicate = directive.param
                    if (testLines.isEmpty()) {
                        if (directive.singleLine) {
                            require(!predicate.isEmpty()) { "$TEST_OUT_DIRECTIVE must be preceded by $TEST_START block or contain test predicate" }
                        } else
                            testLines += readUntil(DIRECTIVE_END)
                    } else {
                        requireSingleLine(directive)
                    }
                    makeTest(testOutLines, lastPgk!!, testLines, predicate)
                    testLines.clear()
                }
                SITE_ROOT_DIRECTIVE -> {
                    requireSingleLine(directive)
                    siteRoot = directive.param
                }
                DOCS_ROOT_DIRECTIVE -> {
                    requireSingleLine(directive)
                    docsRoot = directive.param
                }
                INDEX_DIRECTIVE -> {
                    requireSingleLine(directive)
                    require(siteRoot != null) { "$SITE_ROOT_DIRECTIVE must be specified" }
                    require(docsRoot != null) { "$DOCS_ROOT_DIRECTIVE must be specified" }
                    val indexLines = processApiIndex(siteRoot!!, docsRoot!!, directive.param, remainingApiRefNames)
                    skip = true
                    while (true) {
                        val skipLine = readLine() ?: break@mainLoop
                        if (directive(skipLine) != null) {
                            putBackLine = skipLine
                            break
                        }
                    }
                    skip = false
                    outText += indexLines
                    outText += putBackLine!!
                }
            }
            if (inLine.startsWith(CODE_START)) {
                require(testLines.isEmpty()) { "Previous test was not emitted with $TEST_DIRECTIVE" }
                codeLines += ""
                readUntilTo(CODE_END, codeLines)
                continue@mainLoop
            }
            if (inLine.startsWith(TEST_START)) {
                require(testLines.isEmpty()) { "Previous test was not emitted with $TEST_DIRECTIVE" }
                readUntilTo(TEST_END, testLines)
                continue@mainLoop
            }
            if (inLine.startsWith(SECTION_START) && markdownPart == MarkdownPart.POST_TOC) {
                val i = inLine.indexOf(' ')
                require(i >= 2) { "Invalid section start" }
                val name = inLine.substring(i + 1).trim()
                tocLines += "  ".repeat(i - 2) + "* [$name](#${makeSectionRef(name)})"
                continue@mainLoop
            }
            for (match in API_REF_REGEX.findAll(inLine)) {
                val apiRef = ApiRef(lineNumber, match.groups[2]!!.value)
                allApiRefs += apiRef
                remainingApiRefNames += apiRef.name
            }
            knitRegex?.find(inLine)?.let { knitMatch ->
                val fileName = knitMatch.groups[1]!!.value
                val file = File(markdownFile.parentFile, fileName)
                require(files.add(file)) { "Duplicate file: $file" }
                println("Knitting $file ...")
                val outLines = arrayListOf<String>()
                for (include in includes) {
                    val includeMatch = include.regex.matchEntire(fileName) ?: continue
                    include.lines.forEach { includeLine ->
                        val line = makeReplacements(includeLine, includeMatch)
                        if (line.startsWith(PACKAGE_PREFIX))
                            lastPgk = line.substring(PACKAGE_PREFIX.length).trim()
                        outLines += line
                    }
                }
                outLines += codeLines
                codeLines.clear()
                writeLinesIfNeeded(file, outLines)
            }
        }
    }
    // update markdown file with toc
    val newLines = buildList<String> {
        addAll(markdown.preTocText)
        if (!tocLines.isEmpty()) {
            add("")
            addAll(tocLines)
            add("")
        }
        addAll(markdown.postTocText)
    }
    if (newLines != markdown.inText) writeLines(markdownFile, newLines)
    // check apiRefs
    for (apiRef in allApiRefs) {
        if (apiRef.name in remainingApiRefNames) {
            println("WARNING: $markdownFile: ${apiRef.line}: Broken reference to [${apiRef.name}]")
        }
    }
    // write test output
    flushTestOut(markdownFile.parentFile, testOut, testOutLines)
}

fun makeTest(testOutLines: MutableList<String>, pgk: String, test: List<String>, predicate: String) {
    val funName = buildString {
        var cap = true
        for (c in pgk) {
            if (c == '.') {
                cap = true
            } else {
                append(if (cap) c.toUpperCase() else c)
                cap = false
            }
        }
    }
    testOutLines += ""
    testOutLines += "    @Test"
    testOutLines += "    fun test$funName() {"
    val prefix = "        test { $pgk.main(emptyArray()) }"
    when (predicate) {
        "" -> makeTestLines(testOutLines, prefix, "verifyLines", test)
        STARTS_WITH_PREDICATE -> makeTestLines(testOutLines, prefix, "verifyLinesStartWith", test)
        ARBITRARY_TIME_PREDICATE -> makeTestLines(testOutLines, prefix, "verifyLinesArbitraryTime", test)
        FLEXIBLE_TIME_PREDICATE -> makeTestLines(testOutLines, prefix, "verifyLinesFlexibleTime", test)
        FLEXIBLE_THREAD_PREDICATE -> makeTestLines(testOutLines, prefix, "verifyLinesFlexibleThread", test)
        LINES_START_UNORDERED_PREDICATE -> makeTestLines(testOutLines, prefix, "verifyLinesStartUnordered", test)
        LINES_START_PREDICATE -> makeTestLines(testOutLines, prefix, "verifyLinesStart", test)
        else -> {
            testOutLines += prefix + ".also { lines ->"
            testOutLines += "            check($predicate)"
            testOutLines += "        }"
        }
    }
    testOutLines += "    }"
}

fun makeTestLines(testOutLines: MutableList<String>, prefix: String, method: String, test: List<String>) {
    testOutLines += "$prefix.$method("
    for ((index, testLine) in test.withIndex()) {
        val commaOpt = if (index < test.size - 1) "," else ""
        val escapedLine = testLine.replace("\"", "\\\"")
        testOutLines += "            \"$escapedLine\"$commaOpt"
    }
    testOutLines += "        )"
}

fun makeReplacements(line: String, match: MatchResult): String {
    var result = line
    for ((id, group) in match.groups.withIndex()) {
        if (group != null)
            result = result.replace("\$\$$id", group.value)
    }
    return result
}

fun flushTestOut(parentDir: File?, testOut: String?, testOutLines: MutableList<String>) {
    if (testOut == null) return
    val file = File(parentDir, testOut)
    testOutLines += "}"
    writeLinesIfNeeded(file, testOutLines)
    testOutLines.clear()
}

fun MarkdownTextReader.readUntil(marker: String): List<String> =
        arrayListOf<String>().also { readUntilTo(marker, it) }

fun MarkdownTextReader.readUntilTo(marker: String, list: MutableList<String>) {
    while (true) {
        val line = readLine() ?: break
        if (line.startsWith(marker)) break
        list += line
    }
}

inline fun <T> buildList(block: ArrayList<T>.() -> Unit): List<T> {
    val result = arrayListOf<T>()
    result.block()
    return result
}

fun requireSingleLine(directive: Directive) {
    require(directive.singleLine) { "${directive.name} directive must end on the same line with '$DIRECTIVE_END'" }
}

fun makeSectionRef(name: String): String = name.replace(' ', '-').replace(".", "").toLowerCase()

class Include(val regex: Regex, val lines: MutableList<String> = arrayListOf())

class Directive(
        val name: String,
        val param: String,
        val singleLine: Boolean
)

fun directive(line: String): Directive? {
    if (!line.startsWith(DIRECTIVE_START)) return null
    var s = line.substring(DIRECTIVE_START.length).trim()
    val singleLine = s.endsWith(DIRECTIVE_END)
    if (singleLine) s = s.substring(0, s.length - DIRECTIVE_END.length)
    val i = s.indexOf(' ')
    val name = if (i < 0) s else s.substring(0, i)
    val param = if (i < 0) "" else s.substring(i).trim()
    return Directive(name, param, singleLine)
}

class ApiRef(val line: Int, val name: String)

enum class MarkdownPart { PRE_TOC, TOC, POST_TOC }

class MarkdownTextReader(r: Reader) : LineNumberReader(r) {
    val inText = arrayListOf<String>()
    val preTocText = arrayListOf<String>()
    val postTocText = arrayListOf<String>()
    var markdownPart: MarkdownPart = MarkdownPart.PRE_TOC
    var skip = false

    val outText: MutableList<String> get() = when (markdownPart) {
        MarkdownPart.PRE_TOC -> preTocText
        MarkdownPart.POST_TOC -> postTocText
        else -> throw IllegalStateException("Wrong state: $markdownPart")
    }

    override fun readLine(): String? {
        val line = super.readLine() ?: return null
        inText += line
        if (!skip && markdownPart != MarkdownPart.TOC)
            outText += line
        return line
    }
}

fun <T : LineNumberReader> File.withLineNumberReader(factory: (Reader) -> T, block: T.() -> Unit): T {
    val reader = factory(reader())
    reader.use {
        try {
            it.block()
        } catch (e: IllegalArgumentException) {
            println("ERROR: ${this@withLineNumberReader}: ${it.lineNumber}: ${e.message}")
        }
    }
    return reader
}

fun File.withMarkdownTextReader(block: MarkdownTextReader.() -> Unit): MarkdownTextReader =
        withLineNumberReader<MarkdownTextReader>(::MarkdownTextReader, block)

fun writeLinesIfNeeded(file: File, outLines: List<String>) {
    val oldLines = try {
        file.readLines()
    } catch (e: IOException) {
        emptyList<String>()
    }
    if (outLines != oldLines) writeLines(file, outLines)
}

fun writeLines(file: File, lines: List<String>) {
    println(" Writing $file ...")
    file.parentFile?.mkdirs()
    file.printWriter().use { out ->
        lines.forEach { out.println(it) }
    }
}

data class ApiIndexKey(
        val docsRoot: String,
        val pkg: String
)

val apiIndexCache: MutableMap<ApiIndexKey, Map<String, String>> = HashMap()

val REF_LINE_REGEX = Regex("<a href=\"([a-z/.\\-]+)\">([a-zA-z.]+)</a>")
val INDEX_HTML = "/index.html"
val INDEX_MD = "/index.md"

fun loadApiIndex(
        docsRoot: String,
        path: String,
        pkg: String,
        namePrefix: String = ""
): Map<String, String> {
    val fileName = docsRoot + "/" + path + INDEX_MD
    val visited = mutableSetOf<String>()
    val map = HashMap<String, String>()
    File(fileName).withLineNumberReader<LineNumberReader>(::LineNumberReader) {
        while (true) {
            val line = readLine() ?: break
            val result = REF_LINE_REGEX.matchEntire(line) ?: continue
            val refLink = result.groups[1]!!.value
            if (refLink.startsWith("..")) continue // ignore cross-references
            val refName = namePrefix + result.groups[2]!!.value
            map.put(refName, path + "/" + refLink)
            map.put(pkg + "." + refName, path + "/" + refLink)
            if (refLink.endsWith(INDEX_HTML)) {
                if (visited.add(refLink)) {
                    val path2 = path + "/" + refLink.substring(0, refLink.length - INDEX_HTML.length)
                    map += loadApiIndex(docsRoot, path2, pkg, refName + ".")
                }
            }
        }
    }
    return map
}

fun processApiIndex(
        siteRoot: String,
        docsRoot: String,
        pkg: String,
        remainingApiRefNames: MutableSet<String>
): List<String> {
    val key = ApiIndexKey(docsRoot, pkg)
    val map = apiIndexCache.getOrPut(key, {
        print("Parsing API docs at $docsRoot/$pkg: ")
        val result = loadApiIndex(docsRoot, pkg, pkg)
        println("${result.size} definitions")
        result
    })
    val indexList = arrayListOf<String>()
    val it = remainingApiRefNames.iterator()
    while (it.hasNext()) {
        val refName = it.next()
        val refLink = map[refName] ?: continue
        indexList += "[$refName]: $siteRoot/$refLink"
        it.remove()
    }
    return indexList
}
