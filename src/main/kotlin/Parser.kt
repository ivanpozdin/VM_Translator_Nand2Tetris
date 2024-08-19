package org.example

import java.nio.file.Path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.useLines

// Parses each VM command into its lexical elements
class Parser(private val inputFile: Path) {
    private val lineIterator: MutableIterator<String>
    private lateinit var currentCommand: List<String>
    private var lines = mutableListOf<String>()

    init {
        readLines()
        cleanCommentsAndWhitespaces()
        lineIterator = lines.iterator()
    }


    private fun readLines() {
        inputFile.useLines { sequence ->
            try {
                sequence.iterator().forEachRemaining { line ->
                    lines.add(line)
                }
            } catch (_: CancellationException) {
            }
        }
    }

    private fun cleanCommentsAndWhitespaces() {
        // For every line find "//" and remove everything after that
        for (i in lines.indices) {
            var line = lines[i]
            val commentStart = line.indexOfFirst { it == '/' }
            if (commentStart != -1) {
                line = line.removeRange(commentStart, line.length)
            }
            lines[i] = line.trim()
        }
        lines = lines.filter {
            it.isNotBlank()
        }.toMutableList()
    }

    val hasMoreCommands: Boolean
        get() = lineIterator.hasNext()


    fun advance() {
        currentCommand = lineIterator.next().split(" ")
    }

    val commandType: Command
        get() {
            return when (currentCommand[0]) {
                "add", "sub", "neg", "eq", "lt", "gt", "and", "or", "not" -> Command.ARITHMETIC
                "push" -> Command.PUSH
                "pop" -> Command.POP
//            "label" -> Command.LABEL
//            "goto" -> Command.GOTO
//            "if-goto" -> Command.IF
//            "function" -> Command.FUNCTION
//            "call" -> Command.CALL
//            "return" -> Command.RETURN
                else -> throw IllegalArgumentException("No such command!")
            }
        }

    val arg1: String
        get() {
            if (commandType == Command.ARITHMETIC) {
                return currentCommand[0]
            } else if (commandType == Command.RETURN) {
                throw IllegalArgumentException("Can only be called on arithmetic commands!")
            }
            return currentCommand[1]
        }

    val arg2: String
        get() {
            if (commandType !in listOf(Command.PUSH, Command.POP, Command.FUNCTION, Command.CALL)) {
                throw IllegalArgumentException("Can't be called with this command!")
            }
            return currentCommand[2]
        }
}