package cn.iinti.atom

import cn.iinti.atom.utils.Md5Utils.md5Bytes
import cn.iinti.atom.utils.ResourceUtil.Companion.readLines
import com.google.common.base.Splitter
import lombok.Getter
import lombok.SneakyThrows
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * 此工具，用于对项目符号进行批量改名。改名后生成一个全新的项目，
 */
object ProjectMaker {
    @Throws(Throwable::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // 此代码在idea软件中执行
        val configRules = readLines("make_project.conf.txt")
        val backendProjectRoot = System.getProperty("user.dir")
        Maker.doMake(configRules, File(backendProjectRoot))
    }

    internal object Maker {
        private var genRule: GenRule? = null
        lateinit var rootDirStr: String

        @Throws(Throwable::class)
        fun doMake(configRules: List<String>, srcProjectRoot: File) {
            // 构建规则
            genRule = GenRule.parse(configRules)
            // 扫描输出目录的历史文件，我们需要计算需要被删除的文件的diff
            // diff定义则为，源不存在、目标存在；且不为忽略文件
            genRule!!.scanOutputOldDir()

            rootDirStr = srcProjectRoot.absoluteFile.canonicalPath
            val srcRootDir = File(rootDirStr)
            genRule!!.init(srcRootDir)
            for (ignoreFile in genRule!!.ignoreFiles.runtimeMatchFiles) {
                val relativePath = ignoreFile.canonicalPath.substring(rootDirStr.length + 1)
                genRule!!.root.remove(relativePath)
            }

            // 在输出目录下，需要先扫描一下,这样等会清理数据的时候，就不会把忽略文件删除掉了
            // 否则只有原目录存在，且在原目录命中忽略规则的部分才可以真正命中忽略规则
            ignoreFromOutputDirectory()


            Files.walkFileTree(srcRootDir.toPath(), object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    return handleFile(file, attrs)
                }

                @Throws(IOException::class)
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    return handleFile(dir, attrs)
                }
            })

            // 同步需要被删除的文件
            genRule!!.root.doClean()

            for (cmd in genRule!!.cmds) {
                val rewriteCmd = contentReplace(cmd)
                Shell.execute(rewriteCmd, null, genRule!!.outputRootDir)
            }
        }

        @Throws(IOException::class)
        private fun ignoreFromOutputDirectory() {
            val outputRootDirStr = genRule!!.outputRootDir!!.absoluteFile.canonicalPath
            Files.walkFileTree(genRule!!.outputRootDir!!.toPath(), object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                fun handleFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val file = path.toFile()
                    // 当前文件的相对路径
                    val relativePath = file.canonicalPath.substring(outputRootDirStr.length)
                    val needIgnore = genRule!!.ignoreFiles.match(file)
                    // 文件忽略逻辑
                    if (needIgnore) {
                        genRule!!.root.remove(relativePath)
                        return if (attrs.isDirectory) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
                    }
                    return FileVisitResult.CONTINUE
                }

                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    return handleFile(file, attrs)
                }

                @Throws(IOException::class)
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    return handleFile(dir, attrs)
                }
            })
        }


        @Throws(IOException::class)
        private fun handleFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
            val file = path.toFile()

            //System.out.println("handle file: " + file.getAbsolutePath());

            // 当前文件的相对路径
            val relativePath = file.canonicalPath.substring(rootDirStr!!.length)
            val needIgnore = genRule!!.ignoreFiles.match(file)
            // 文件忽略逻辑
            if (needIgnore) {
                genRule!!.root.remove(relativePath)
                return if (attrs.isDirectory) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
            }

            // 实际上只处理文件
            if (attrs.isDirectory) {
                // 文件夹的话，需要来计算keep规则
                genRule!!.keepFiles.match(file)
                genRule!!.notOverwrite.match(file)
                return FileVisitResult.CONTINUE
            }

            genRule!!.root.remove(relativePath)

            if (genRule!!.keepFiles.match(file)) {
                // 如果是keep文件，那么直接复制过去，这个行为主要为了应对二进制文件，避免对二进制执行replace操作
                // 因为我们会把所有打算修改的文件使用文本方式载入到内存
                val relocatedKeepFile = contentReplace(relativePath)
                val target = File(genRule!!.outputRootDir, relocatedKeepFile)
                FileUtils.copyFile(file, target)
                genRule!!.root.remove(relocatedKeepFile)
                return FileVisitResult.CONTINUE
            }

            if (genRule!!.notOverwrite.match(file)) {
                val target = File(genRule!!.outputRootDir, contentReplace(relativePath))
                if (target.exists()) {
                    // 如果存在，则不能覆盖
                    return FileVisitResult.CONTINUE
                }
            }


            // 输出文件路径
            val outFile = calcOutFile(FileUtils.readLines(file, StandardCharsets.UTF_8), relativePath)
            val newRelativePath = outFile.canonicalPath.substring(genRule!!.outputRootDir!!.canonicalPath.length + 1)
            genRule!!.root.remove(newRelativePath)

            FileUtils.forceMkdirParent(outFile)

            // 执行内容的replace规则
            var content = contentReplace(FileUtils.readFileToString(file, StandardCharsets.UTF_8))

            for (shuffle in genRule!!.shuffles) {
                content = shuffle.performShuffle(file, content)
            }

            FileUtils.write(outFile, content, StandardCharsets.UTF_8)
            println("write file: " + outFile.absolutePath)
            return FileVisitResult.CONTINUE
        }

        private val patterns = arrayOf(
            Pattern.compile("package\\s+(\\S+).*?;"),
            Pattern.compile("package\\s+(\\S+)")
        )

        // java: 普通java文件
        // aidl：Android上的跨进程通行协议定义文件
        // groovy：java变种，我们比较多的使用groovy描述动态规则
        // kts：kotlin，java变种，在Android项目中比较容易遇到
        private val javaLikeFiles = arrayOf(".java", ".aidl", ".groovy", ".kts")

        private fun calcOutFile(lines: List<String?>, relativePath: String): File {
            // 对于java和aidl文件，我们需要修改包名，此时需要重新计算文件路径
            // 对于其他类型文件，我们只会考虑修改文件内容，不会修改文件路径规则
            var isJavaLikeFile = false
            if (!relativePath.endsWith(".gradle.kts")) { // .gradle.kts是构建脚本，没有包名的概念
                for (str in javaLikeFiles) {
                    if (relativePath.endsWith(str)) {
                        isJavaLikeFile = true
                        break
                    }
                }
            }

            if (!isJavaLikeFile) {
                return File(genRule!!.outputRootDir, contentReplace(relativePath))
            }

            val contents = arrayOf(removeJavaComment(lines), StringUtils.join(lines, "\n"))

            var matcher: Matcher? = null
            for (content in contents) {
                for (pattern in patterns) {
                    val tmpMatcher = pattern.matcher(content)
                    if (tmpMatcher.find()) {
                        matcher = tmpMatcher
                        break
                    }
                }
                if (matcher != null) {
                    break
                }
            }

            if (matcher == null) {
                println("failed to handle java like file: $relativePath")
                return File(genRule!!.outputRootDir, contentReplace(relativePath))
            }


            // 通过正则规则抽取了
            val pkg = matcher.group(1)

            var nowRule: String? = null
            for (str in genRule!!.pkgRenameRules.keys) {
                if (!pkg.startsWith(str)) {
                    continue
                }
                if (nowRule == null) {
                    nowRule = str
                } else if (str.length > nowRule.length) {
                    // 长路径规则优先
                    nowRule = str
                }
            }
            val targetRelatePath = if (nowRule == null) relativePath else relativePath.replace(
                nowRule.replace(".", "/"),
                genRule!!.pkgRenameRules[nowRule]!!.replace(".", "/")
            )
            // 文件名称也需要修改
            return File(genRule!!.outputRootDir, contentReplace(targetRelatePath))
        }


        private fun contentReplace(input: String): String {
            var input = input
            for ((key, value) in genRule!!.keepContents) {
                input = input.replace(key, value)
            }
            // 文件名称也需要修改
            for ((key, value) in genRule!!.keywordReplaceRules) {
                input = input.replace(key.str, value)
            }
            for ((key, value) in genRule!!.keepContents) {
                input = input.replace(value, key)
            }
            return input
        }


        private fun removeJavaComment(content: List<String?>): String {
            val sb = StringBuilder()
            var state = 0 // 0:begin, 1:multi line comment
            var tempStr: String? = null
            var i = 0
            while (i < content.size) {
                var handleStr: String?
                if (tempStr != null) {
                    handleStr = tempStr
                    tempStr = null
                } else {
                    handleStr = content[i]
                }
                when (state) {
                    0 -> {
                        handleStr = handleStr!!.trim { it <= ' ' }
                        if (handleStr.startsWith("//")) {
                            i++
                            continue
                        }
                        if (handleStr.startsWith("/*")) {
                            val index = handleStr.indexOf("*/")
                            if (index >= 2) {
                                handleStr = handleStr.substring(index + 2)
                                if (StringUtils.isNotBlank(handleStr)) {
                                    tempStr = handleStr
                                    i--
                                }
                                i++
                                continue
                            }
                            state = 1
                        } else {
                            sb.append(handleStr)
                            sb.append("\n")
                        }
                        i++
                        continue
                    }

                    1 -> {
                        val index = handleStr!!.indexOf("*/")
                        if (index >= 0) {
                            handleStr = handleStr.substring(index + 2)
                            if (StringUtils.isNotBlank(handleStr)) {
                                tempStr = handleStr
                                i--
                            }
                            state = 0
                        }
                    }
                }
                i++
            }
            return sb.toString()
        }
    }


    internal object Shell {
        @Throws(IOException::class, InterruptedException::class)
        fun execute(cmd: String, envp: Array<String?>?, dir: File?) {
            println("execute cmd: $cmd")
            val process = Runtime.getRuntime().exec(cmd, envp, dir)
            InputStreamPrintThread(process.errorStream)
            InputStreamPrintThread(process.inputStream)
            process.waitFor()
        }

        class InputStreamPrintThread(private val inputStream: InputStream) : Thread() {
            init {
                start()
            }

            override fun run() {
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                try {
                    while ((bufferedReader.readLine().also { line = it }) != null) {
                        println(line)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    internal class MultiConfigRule private constructor() {
        private val config: MutableMap<String, MutableList<String>> = HashMap()

        fun interface ConfigItemHandler {
            fun handle(item: String)
        }

        fun accessValue(key: String, handler: ConfigItemHandler): MultiConfigRule {
            for (value in getList(key)) {
                handler.handle(value)
            }
            return this
        }

        fun getList(key: String): List<String> {
            val strings: List<String> = config[key.uppercase(Locale.getDefault())]!!
                ?: return emptyList()
            return strings
        }

        private fun parseLine(line: String) {
            val index = line.indexOf(":")
            if (index <= 0) {
                println("error config:$line")
                return
            }

            val cmd = line.substring(0, index).trim { it <= ' ' }.uppercase(Locale.getDefault())
            val content = line.substring(index + 1).trim { it <= ' ' }
            config.computeIfAbsent(cmd) { k: String? -> ArrayList() }.add(content)
        }

        companion object {
            fun parse(configRules: List<String>): MultiConfigRule {
                val multiConfigRule = MultiConfigRule()
                for (line in configRules) {
                    var line = line
                    line = line.trim { it <= ' ' }
                    if (StringUtils.isBlank(line) || line.startsWith("#")) {
                        // comment
                        continue
                    }
                    multiConfigRule.parseLine(line)
                }
                return multiConfigRule
            }
        }
    }

    internal class FileMatcher {
        private val rawRules: MutableSet<String> = HashSet()

        val runtimeMatchFiles: MutableSet<File> = HashSet()
        private val runtimeMatchDirs: MutableSet<File> = HashSet()

        fun addRule(rule: String) {
            rawRules.add(rule.trim { it <= ' ' })
        }

        fun resolveRootDir(root: File?) {
            // 所有绝对路径的的忽略规则
            val iterator = rawRules.iterator()
            while (iterator.hasNext()) {
                val ignoreConfig = iterator.next()
                if (ignoreConfig.startsWith("/")) {
                    runtimeMatchFiles.add(File(root, ignoreConfig.substring(1)).absoluteFile)
                    iterator.remove()
                }
            }
        }

        private fun matchWithoutRuntimeRule(file: File): Boolean {
            if (runtimeMatchFiles.contains(file)) {
                return true
            }
            for (dir in runtimeMatchDirs) {
                // 判断是否是目标文件夹中的文件的子目录
                val basePath = dir.absolutePath
                if (file.absolutePath == basePath) {
                    return true
                }
                if (file.absolutePath.startsWith("$basePath/")) {
                    return true
                }
            }
            return false
        }

        fun match(file: File): Boolean {
            if (matchWithoutRuntimeRule(file)) {
                return true
            }
            var hasWildcardMatch = false
            for (config in rawRules) {
                var config = config
                var onlyDirectory = false
                if (config.endsWith("/")) {
                    onlyDirectory = true
                    config = config.substring(0, config.length - 1)
                }
                if (!onlyDirectory && file.isFile && file.name == config) {
                    runtimeMatchFiles.add(file)
                }
                if (config.startsWith("*.")) {
                    val suffix = config.substring(2)
                    if (file.name.endsWith(suffix)) {
                        runtimeMatchFiles.add(file)
                        hasWildcardMatch = true
                    }
                } else {
                    var parentFile = file.parentFile
                    while (parentFile != null) {
                        val candidate = parentFile.toPath().resolve(config).toFile()
                        if (candidate.exists()) {
                            if (candidate.isDirectory) {
                                runtimeMatchDirs.add(candidate)
                            } else {
                                runtimeMatchFiles.add(candidate)
                            }
                        }
                        parentFile = parentFile.parentFile
                    }
                }
            }
            if (hasWildcardMatch) {
                return true
            }
            return matchWithoutRuntimeRule(file)
        }
    }

    internal class GenRule {
        var pkgRenameRules: MutableMap<String, String> = HashMap()
        var keywordReplaceRules: MutableMap<KeywordString, String> = TreeMap()

        class KeywordString(@field:Getter val str: String) : Comparable<KeywordString> {
            private val seq: Int

            init {
                seq = inc.incrementAndGet()
            }

            override fun compareTo(o: KeywordString): Int {
                val priority1 = specialPriority(str)
                val priority2 = specialPriority(o.str)
                if (priority1 != priority2) {
                    return priority2 - priority1
                }
                return seq.compareTo(o.seq)
            }

            override fun toString(): String {
                return "$str:$seq"
            }

            companion object {
                private val inc = AtomicInteger(0)
                private val registry: MutableMap<String, KeywordString> = ConcurrentHashMap()

                fun valueOf(key: String): KeywordString {
                    var keywordString = registry[key]
                    if (keywordString != null) {
                        return keywordString
                    }
                    synchronized(KeywordString::class.java) {
                        keywordString = registry[key]
                        if (keywordString != null) {
                            return keywordString!!
                        }
                        keywordString = KeywordString(key)
                        registry.put(key, keywordString!!)
                    }
                    return keywordString!!
                }

                private fun specialPriority(str: String): Int {
                    // java文件路径：src/main/java/cn/iinti/atom/service/base/env/Constants.java
                    if (str.contains("/")) {
                        return str.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size + 500
                    }
                    // 包名需要高优先级替换
                    if (str.contains(".")) {
                        return str.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size + 1000
                    }
                    // jni声明也属于包名
                    if (str.startsWith("Java_")) {
                        return str.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size + 2000
                    }
                    return 0
                }
            }
        }


        var ignoreFiles: FileMatcher = FileMatcher()
        var keepFiles: FileMatcher = FileMatcher()
        var notOverwrite: FileMatcher = FileMatcher()
        var shuffles: MutableList<Shuffle> = ArrayList()
        var keepContents: MutableMap<String, String> = HashMap()
        var cmds: MutableList<String> = ArrayList()

        var root: FSNode = FSNode("root", true, null)
        var outputRootDir: File? = null

        fun init(root: File?) {
            ignoreFiles.resolveRootDir(root)
            keepFiles.resolveRootDir(root)
            notOverwrite.resolveRootDir(root)

            // java的替换规则，需要产生对应的keyword规则
            val toPkgSet = TreeSet<String>()
            for ((from, to) in pkgRenameRules) {
                toPkgSet.add(to.trim { it <= ' ' })

                keywordReplaceRules[KeywordString.valueOf(from)] = to
                keywordReplaceRules[KeywordString.valueOf(pkg2JniName(from))] =
                    pkg2JniName(to)
                keywordReplaceRules[KeywordString.valueOf(pkg2PathName(from))] =
                    pkg2PathName(to)
            }

            val genId = if (toPkgSet.isEmpty()) outputRootDir!!.name else StringUtils.join(toPkgSet)
            shuffles.forEach(Consumer { shuffle: Shuffle -> shuffle.init(root, genId) })
        }

        @Throws(IOException::class)
        fun scanOutputOldDir() {
            if (!outputRootDir!!.exists()) {
                FileUtils.forceMkdir(outputRootDir)
                return
            }
            // 所有已知文件，把他搞到内存中
            val rootDirStr = outputRootDir!!.canonicalPath
            Files.walkFileTree(outputRootDir!!.toPath(), object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val relativePath = file.toFile().canonicalPath.substring(rootDirStr.length + 1)
                    root.addChild(relativePath, false, file.toFile())
                    return FileVisitResult.CONTINUE
                }
            })
        }


        class FSNode(private val name: String, private val isDir: Boolean, private val file: File?) {
            private var children: MutableMap<String, FSNode>? = null

            init {
                if (isDir) {
                    children = LinkedHashMap()
                }
            }

            fun addChild(path: String, isDir: Boolean, file: File?) {
                var path = path
                var isDir = isDir
                check(this.isDir) { "can not add new " }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length - 1)
                    isDir = true
                }
                val index = path.indexOf("/")
                if (index < 0) {
                    children!![path] = FSNode(path, isDir, file)
                } else {
                    val dir = path.substring(0, index)
                    val left = path.substring(index + 1)

                    val child = children!!.computeIfAbsent(dir) { d: String -> FSNode(d, true, null) }
                    child.addChild(left, isDir, file)
                }
            }

            fun doClean() {
                if (!isDir && file != null) {
                    println("remove file: " + file.absolutePath)
                    FileUtils.deleteQuietly(file)
                }
                if (isDir) {
                    for (child in children!!.values) {
                        child.doClean()
                    }
                    if (file != null) {
                        println("remove dir: " + file.absolutePath)
                    }
                    FileUtils.deleteQuietly(file)
                }
            }


            override fun equals(o: Any?): Boolean {
                if (this === o) return true
                if (o == null || javaClass != o.javaClass) return false
                val fsNode = o as FSNode
                return name == fsNode.name
            }

            override fun hashCode(): Int {
                return Objects.hash(name)
            }

            fun remove(path: String) {
                var path = path
                if (path.startsWith("/")) {
                    path = path.substring(1)
                }
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length - 1)
                }
                val index = path.indexOf("/")
                if (index < 0) {
                    children!!.remove(path)
                    return
                }
                val dir = path.substring(0, index)
                val left = path.substring(index + 1)

                val child = children!![dir] ?: return
                child.remove(left)
                if (child.isDir && child.children!!.isEmpty()) {
                    children!!.remove(dir)
                }
            }
        }

        companion object {
            fun parse(configRules: List<String>): GenRule {
                val genRule = GenRule()
                val multiConfigRule = MultiConfigRule.parse(configRules)
                multiConfigRule
                    .accessValue("PKG") { item: String ->
                        val pkgRule = item.split("->".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        genRule.pkgRenameRules.put(pkgRule[0].trim { it <= ' ' }, pkgRule[1].trim { it <= ' ' })
                    }
                    .accessValue("KEY") { item: String ->
                        val keyRule = item.split("->".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        genRule.keywordReplaceRules.put(
                            KeywordString.valueOf(keyRule[0].trim { it <= ' ' }),
                            keyRule[1].trim { it <= ' ' })
                    }
                    .accessValue("TARGET") { item: String ->
                        genRule.outputRootDir = resolveFile(item.trim { it <= ' ' })
                    }
                    .accessValue("IGNORE") { item: String -> genRule.ignoreFiles.addRule(item.trim { it <= ' ' }) }
                    .accessValue("KEEP") { item: String -> genRule.keepFiles.addRule(item.trim { it <= ' ' }) }
                    .accessValue("NOTOVERWRITE") { item: String -> genRule.notOverwrite.addRule(item.trim { it <= ' ' }) }
                    .accessValue("KEEPCONTENT") { item: String ->
                        genRule.keepContents.put(
                            item.trim { it <= ' ' },
                            UUID.randomUUID().toString()
                        )
                    }
                    .accessValue("CMD") { item: String -> genRule.cmds.add(item) }
                    .accessValue("SHUFFLE") { item: String ->
                        val rule = item.split("->".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        genRule.shuffles.add(Shuffle(rule[0], rule[1]))
                    }
                return genRule
            }

            @SneakyThrows
            fun resolveFile(path: String): File {
                return path.trim().let {
                    if (it.startsWith("~/")) {
                        FileUtils.getUserDirectoryPath() +
                                path.substring(1)
                    } else {
                        it
                    }
                }.let {
                    File(it).canonicalFile
                }
            }

            private fun pkg2PathName(pkg: String): String {
                return pkg.replace('.', '/')
            }

            private fun pkg2JniName(pkg: String): String {
                return "Java_" + pkg.replace("\\.".toRegex(), "_")
            }
        }
    }

    class Shuffle(keywords: String, patterns: String) {
        private val keywords = keywords.trim { it <= ' ' }
        private var replace: String? = null
        private val fileMatcher = FileMatcher()

        init {
            for (pattern in Splitter.on(',').trimResults()
                .omitEmptyStrings().split(patterns)) {
                fileMatcher.addRule(pattern)
            }
        }

        fun init(root: File?, id: String) {
            this.replace = shuffle(keywords, id)
            fileMatcher.resolveRootDir(root)
        }

        fun performShuffle(path: File, input: String): String {
            if (fileMatcher.match(path)) {
                return input.replace(keywords.toRegex(), replace!!)
            }
            return input
        }

        companion object {
            fun shuffle(input: String, key: String): String {
                val digest = md5Bytes(input + "_" + key)
                val seedWithSalt = ((digest[0].toLong() and 0xFFL) shl 56 or ((digest[1].toLong() and 0xFFL) shl 48
                        ) or ((digest[2].toLong() and 0xFFL) shl 40
                        ) or ((digest[3].toLong() and 0xFFL) shl 32
                        ) or ((digest[4].toLong() and 0xFFL) shl 24
                        ) or ((digest[5].toLong() and 0xFFL) shl 16
                        ) or ((digest[6].toLong() and 0xFFL) shl 8
                        ) or (digest[7].toLong() and 0xFFL))

                val random = Random(seedWithSalt)
                val output = input.toCharArray()
                for (i in output.indices) {
                    if (Character.isDigit(output[i])) {
                        var ch = ('0'.code + random.nextInt(10)).toChar()
                        if (i == 0 && ch == '0') {
                            // 如果是数字，那么第一位不能位0
                            ch = '2'
                        }
                        output[i] = ch
                    } else if (isHex(output[i])) {
                        val ch = ('a'.code + random.nextInt('f'.code - 'a'.code)).toChar()
                        output[i] = ch
                    } else if (isUpperHex(output[i])) {
                        output[i] = ('A'.code + random.nextInt('F'.code - 'A'.code)).toChar()
                    } else if (Character.isLetter(output[i])) {
                        val aAStart = if (random.nextInt(2) % 2 == 0) 65 else 97 //取得大写字母还是小写字母
                        val ch = (aAStart + random.nextInt(26)).toChar()
                        output[i] = ch
                    }
                    //其他字符串，不是数字或者字母。下划线，连接线，其他特殊字符保留原来的模样
                }
                return String(output)
            }

            private fun isHex(ch: Char): Boolean {
                return ch >= 'a' && ch <= 'f'
            }

            private fun isUpperHex(ch: Char): Boolean {
                return ch >= 'A' && ch <= 'F'
            }
        }
    }
}
