package cn.iinti.katom.base.metric.mql.compile

import cn.iinti.katom.base.metric.mql.Context
import cn.iinti.katom.base.metric.mql.MQL
import cn.iinti.katom.base.metric.mql.MQL.VarStatement
import cn.iinti.katom.base.metric.mql.MQL.VoidFunCallStatement
import cn.iinti.katom.base.metric.mql.MetricOperator
import cn.iinti.katom.base.metric.mql.func.FuncFilter
import cn.iinti.katom.base.metric.mql.func.FuncGetVar
import cn.iinti.katom.base.metric.mql.func.MQLFunction
import cn.iinti.katom.utils.Md5Utils.md5Hex
import com.google.common.collect.Lists
import lombok.SneakyThrows
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.io.LineNumberReader
import java.io.StringReader
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function


object MQLCompiler {
    private const val MAC_CACHE = 1024

    private val cache: MutableMap<String, MQL> =
        Collections.synchronizedMap(object : LinkedHashMap<String, MQL>() {
            override fun removeEldestEntry(eldest: Map.Entry<String, MQL>): Boolean {
                return size > MAC_CACHE
            }
        })

    
    fun compile(script: String): MQL {
        check(!StringUtils.isBlank(script)) { "empty script content" }
        return cache.computeIfAbsent(md5Hex(script)) { s: String? -> doCompile(script) }
    }

    @SneakyThrows
    fun doCompile(script: String): MQL {
        val tokenReader = WordReader(
            LineNumberReader(StringReader(script)), "mql main script"
        )
        val statements: MutableList<MQL.Statement> = Lists.newArrayList()
        var firstToken: String? = tokenReader.nextWord() ?: throw BadGrammarException("empty script code")
        while (firstToken != null) {
            if (firstToken == ";") {
                // empty statement
                continue
            }
            val op = tokenReader.nextWord()
                ?: throw BadGrammarException("compile failed, bad code at: " + tokenReader.lineLocationDescription())
            when (op) {
                "(" -> {
                    // this is a None variable declare function call
                    // show(successRate);
                    tokenReader.pushBack("(")
                    tokenReader.pushBack(firstToken)
                    val function = parseExpression(tokenReader)
                    statements.add(VoidFunCallStatement(function))
                }

                "=" -> {
                    // this is a variable declare
                    // taskEnd = aggregate(taskEnd,'serverId');
                    val exp = parseExpression(tokenReader)
                    statements.add(VarStatement(firstToken, exp))
                }

                else -> {
                    throw BadGrammarException(
                        """
                                unexpected token: $firstToken
                                ${tokenReader.locationDescription()}
                                """.trimIndent()
                    )
                }
            }

            firstToken = tokenReader.nextWord()
            if (firstToken == null) {
                break
            }
            while (firstToken == ";") {
                firstToken = tokenReader.nextWord()
                if (firstToken == null) {
                    break
                }
            }
        }

        return MQL(statements)
    }

    @Throws(IOException::class)
    private fun parseExpression(tokenReader: WordReader): Function<Context, Any?> {
        val tokenStream: MutableList<String> = Lists.newArrayList()

        val funcDefine: MutableMap<String, List<String>> = HashMap()
        val filterDefine: MutableMap<String, List<String>> = HashMap()
        while (true) {
            val token = tokenReader.nextWord()
            if (token == null || token == ";") {
                break
            }
            if (tokenStream.isEmpty()) {
                // first token
                tokenStream.add(token)
                continue
            }
            val preToken = tokenStream[tokenStream.size - 1]
            if (token == "(") {
                if (StringUtils.equalsAny(preToken, "+", "-", "*", "/", "(")) {
                    // this is an expression
                    tokenStream.add(token)
                    continue
                }
                if (MQLFunction.isFunctionNotDefined(preToken)) {
                    throw BadGrammarException(
                        """no function "$preToken" defined ${tokenReader.locationDescription()}"""
                    )
                }
                // this is a function call
                val sb = StringBuilder(preToken)
                sb.append("(")

                val funcBody: MutableList<String> = Lists.newArrayList()
                funcBody.add(preToken)
                while (true) {
                    val param = tokenReader.nextWord()
                    if (param == null || param == ";") {
                        throw BadGrammarException("error function call: " + token + ", " + tokenReader.locationDescription())
                    }
                    if (param == ")") {
                        sb.append(param)
                        break
                    }
                    sb.append(param)
                    if ("," != param) {
                        funcBody.add(param)
                    }
                }
                val `fun` = sb.toString()

                tokenStream[tokenStream.size - 1] = `fun`
                funcDefine[`fun`] = funcBody
            } else if (token == "[") {
                // this is a variable filter
                val sb = StringBuilder(preToken)
                sb.append("[")

                val filterExp: MutableList<String> = Lists.newArrayList()
                filterExp.add(preToken)

                while (true) {
                    val param = tokenReader.nextWord()
                        ?: throw BadGrammarException("error function call: " + token + ", " + tokenReader.locationDescription())
                    if (param == "]") {
                        sb.append(param)
                        break
                    }
                    sb.append(param)
                    if ("," != param && "=" != param) {
                        filterExp.add(param)
                    }
                }
                val filterExpStr = sb.toString()
                tokenStream[tokenStream.size - 1] = filterExpStr
                filterDefine[filterExpStr] = filterExp
            } else {
                if (preToken == "-" && (tokenStream.size == 1 || (StringUtils.equalsAny(
                        tokenStream[tokenStream.size - 2], "+", "-", "*", "/", "("
                    ))
                            )
                ) {
                    tokenStream[tokenStream.size - 1] = "-$token"
                } else {
                    tokenStream.add(token)
                }
            }
        }

        val reversePolishNotation: MutableList<String> = Lists.newArrayList()
        val tmpOpStack = Stack<Op>()
        tmpOpStack.push(Op.FUCK)

        for (token in tokenStream) {
            if (token == "(") {
                tmpOpStack.push(Op.FUCK_BRACKETS)
                continue
            }
            val op = Op.getBySymbol(token)
            if (op != null) {
                while (true) {
                    val preOp = tmpOpStack.peek()
                    if (preOp == Op.FUCK_BRACKETS) {
                        tmpOpStack.push(op)
                        break
                    }
                    val i = op.priority.compareTo(preOp.priority)
                    if (i <= 0) {
                        tmpOpStack.pop()
                        reversePolishNotation.add(preOp.symbol)
                        continue
                    }

                    tmpOpStack.push(op)
                    break
                }
            } else if (token == ")") {
                while (true) {
                    val pop = tmpOpStack.pop()
                    if (pop == Op.FUCK_BRACKETS) {
                        break
                    }
                    reversePolishNotation.add(pop.symbol)
                }
            } else {
                reversePolishNotation.add(token)
            }
        }

        while (tmpOpStack.peek() != Op.FUCK) {
            reversePolishNotation.add(tmpOpStack.pop().symbol)
        }

        val computeStack = Stack<Function<Context, Any?>>()
        for (token in reversePolishNotation) {
            val op = Op.getBySymbol(token)
            if (op != null) {
                val right = computeStack.pop()
                val left = computeStack.pop()
                computeStack.push {
                    op.func!!.apply(left, right)
                }
                continue
            }

            val funcBody = funcDefine[token]
            if (funcBody != null) {
                computeStack.push(
                    MQLFunction.createFunction(funcBody[0], funcBody.subList(1, funcBody.size)).asOpNode()
                )
                continue
            }
            val filterExp = filterDefine[token]
            if (filterExp != null) {
                // a [p1=v1,p2=v2]
                computeStack.push(FuncFilter(filterExp).asOpNode())
                continue
            }

            val funcGetVar = FuncGetVar(Lists.newArrayList(token))
            computeStack.push { context: Context ->
                // call a variable
                val mqlVar = funcGetVar.call(context)
                if (mqlVar != null) {
                    return@push mqlVar
                }
                token.toDouble()
            }
        }
        return computeStack.pop()
    }


    private enum class Op(
        val symbol: String,
        val priority: Int,
        val func: BiFunction<Function<Context, Any?>, Function<Context, Any?>, MetricOperator>?
    ) {
        FUCK_BRACKETS("(", 0, null),
        FUCK("#", 0, null),
        ADD(
            "+", 10,
            { leftParam, rightParam ->
                MetricOperator.add(
                    leftParam,
                    rightParam
                )
            }),
        MINUS(
            "-", 10,
            { leftParam, rightParam ->
                MetricOperator.minus(
                    leftParam,
                    rightParam
                )
            }),
        MULTIPLE(
            "*", 100,
            { leftParam, rightParam ->
                MetricOperator.multiply(
                    leftParam,
                    rightParam
                )
            }),
        DIVIDE(
            "/", 100,
            { leftParam, rightParam ->
                MetricOperator.divide(
                    leftParam,
                    rightParam
                )
            });


        companion object {
            fun getBySymbol(token: String): Op? {
                for (op in entries) {
                    if (token == op.symbol) {
                        return op
                    }
                }
                return null
            }
        }
    }


    class BadGrammarException(message: String?) : RuntimeException(message)
}
