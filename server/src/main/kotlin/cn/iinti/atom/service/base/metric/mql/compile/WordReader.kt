package cn.iinti.atom.service.base.metric.mql.compile

import java.io.File
import java.io.IOException
import java.io.LineNumberReader
import java.util.*


/**
 * An abstract reader of words, with the possibility to include other readers.
 * Words are separated by spaces or broken off at delimiters. Words containing
 * spaces or delimiters can be quoted with single or double quotes.
 * Comments (everything starting with '#' on a single line) are ignored.
 *
 * @author Eric Lafortune
 * @author iinti
 */
class WordReader(private val reader: LineNumberReader?, private val description: String) {
    private var currentLine: String? = null
    private var currentLineLength = 0
    private var currentIndex = 0
    private var currentWord: String? = null
    private val stageWords = LinkedList<String>()

    private var currentComments: String? = null

    @Throws(IOException::class)
    fun nextWord(): String? {
        val ret = nextWordInternal()
        if (printToken) {
            println(ret)
        }
        return ret
    }

    @Throws(IOException::class)
    private fun nextWordInternal(): String? {
        if (!stageWords.isEmpty()) {
            return stageWords.poll()
        }
        return nextWordImpl()
    }

    /**
     * Reads a word from this WordReader, or from one of its active included
     * WordReader objects.
     *
     * @return the read word.
     */
    @Throws(IOException::class)
    private fun nextWordImpl(): String? {
        currentWord = null

        // Get a word from this reader.

        // Skip any whitespace and comments left on the current line.
        if (currentLine != null) {
            // Skip any leading whitespace.
            while (currentIndex < currentLineLength &&
                Character.isWhitespace(currentLine!![currentIndex])
            ) {
                currentIndex++
            }

            // Skip any comments.
            if (currentIndex < currentLineLength &&
                isComment(currentLine!![currentIndex])
            ) {
                currentIndex = currentLineLength
            }
        }

        // Make sure we have a non-blank line.
        while (currentLine == null || currentIndex == currentLineLength) {
            currentLine = nextLine()
            if (currentLine == null) {
                return null
            }

            currentLineLength = currentLine!!.length

            // Skip any leading whitespace.
            currentIndex = 0
            while (currentIndex < currentLineLength &&
                Character.isWhitespace(currentLine!![currentIndex])
            ) {
                currentIndex++
            }

            // Remember any leading comments.
            if (currentIndex < currentLineLength &&
                isComment(currentLine!![currentIndex])
            ) {
                // Remember the comments.
                val comment = currentLine!!.substring(currentIndex + 1)
                currentComments = if (currentComments == null) comment else """
     $currentComments
     $comment
     """.trimIndent()

                // Skip the comments.
                currentIndex = currentLineLength
            }
        }

        // Find the word starting at the current index.
        var startIndex = currentIndex
        val endIndex: Int

        val startChar = currentLine!![startIndex]

        if (isQuote(startChar)) {
            // The next word is starting with a quote character.
            // Skip the opening quote.
            startIndex++

            // The next word is a quoted character string.
            // Find the closing quote.
            do {
                currentIndex++

                if (currentIndex == currentLineLength) {
                    currentWord = currentLine!!.substring(startIndex - 1, currentIndex)
                    throw IOException("Missing closing quote for " + locationDescription())
                }
            } while (currentLine!![currentIndex] != startChar)

            endIndex = currentIndex++
        } else if (isDelimiter(startChar)) {
            // The next word is a single delimiting character.
            endIndex = ++currentIndex
        } else {
            // The next word is a simple character string.
            // Find the end of the line, the first delimiter, or the first
            // white space.
            while (currentIndex < currentLineLength) {
                val currentCharacter = currentLine!![currentIndex]
                if (isNonStartDelimiter(currentCharacter) ||
                    Character.isWhitespace(currentCharacter) ||
                    isComment(currentCharacter)
                ) {
                    break
                }

                currentIndex++
            }

            endIndex = currentIndex
        }

        // Remember and return the parsed word.
        currentWord = currentLine!!.substring(startIndex, endIndex)

        if (currentIndex == currentLineLength && ";" != currentWord) {
            // auto append a separator at end of line'
            stageWords.push(";")
        }
        return currentWord
    }


    /**
     * Returns the comments collected before returning the last word.
     * Starts collecting new comments.
     *
     * @return the collected comments, or `null` if there weren't any.
     */
    @Throws(IOException::class)
    fun lastComments(): String? {
        val comments = currentComments
        currentComments = null
        return comments
    }

    fun peekCurrentWord(): String? {
        return if (stageWords.isEmpty()) currentWord else stageWords.peek()
    }

    fun pushBack(word: String) {
        stageWords.push(word)
    }

    /**
     * Constructs a readable description of the current position in this
     * WordReader and its included WordReader objects.
     *
     * @return the description.
     */
    fun locationDescription(): String {
        val currentWord = peekCurrentWord()
        return (if (currentWord == null) "end of " else "'$currentWord' in ") +
                lineLocationDescription()
    }


    // Small utility methods.
    private fun isComment(character: Char): Boolean {
        return character == COMMENT_CHARACTER
    }


    private fun isDelimiter(character: Char): Boolean {
        return isStartDelimiter(character) || isNonStartDelimiter(character)
    }


    private fun isStartDelimiter(character: Char): Boolean {
        return character == '@'
    }


    private fun isNonStartDelimiter(character: Char): Boolean {
        return character == '{' || character == '}' || character == '(' || character == ')' || character == ',' || character == ';' || character == '=' || character == '[' || character == ']' || character == '+' || character == '-' || character == '*' || character == '/' || character == File.pathSeparatorChar
    }


    private fun isQuote(character: Char): Boolean {
        return character == '\'' ||
                character == '"'
    }


    /**
     * Reads a line from this WordReader, or from one of its active included
     * WordReader objects.
     *
     * @return the read line.
     */
    @Throws(IOException::class)
    protected fun nextLine(): String {
        return reader!!.readLine()
    }

    /**
     * Returns a readable description of the current WordReader position.
     *
     * @return the description.
     */
    fun lineLocationDescription(): String {
        return "line " + reader!!.lineNumber + " of " + description
    }


    @Throws(IOException::class)
    fun close() {
        reader?.close()
    }

    companion object {
        private const val COMMENT_CHARACTER = '#'
        private const val printToken = false
    }
}
