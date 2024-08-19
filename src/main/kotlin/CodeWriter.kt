package org.example

import java.io.File

// Writes the assembly code that implements the parsed command
class CodeWriter {
    private lateinit var currentFile: File

    private var counter = 0

    fun setFileName(fileName: String) {
        currentFile = File(fileName)
        currentFile.writeText("")
        counter = 0
    }

    fun writeArithmetic(command: String) {
        counter += 1
        val codeToWrite = "// $command\n" + when (command) {
            "add" -> addSubAndOr("+")
            "sub" -> addSubAndOr("-")
            "neg" -> negNot("-")
            "eq" -> equalGreaterLess("=")
            "gt" -> equalGreaterLess(">")
            "lt" -> equalGreaterLess("<")
            "and" -> addSubAndOr("&")
            "or" -> addSubAndOr("|")
            "not" -> negNot("!")
            else -> throw IllegalArgumentException("No such command!")
        }
        currentFile.appendText(codeToWrite)
    }

    fun writePushPop(command: Command, segment: String, index: Int) {
        val codeToWrite =
            if (command == Command.POP) {
                when (segment) {
                    in segmentsVirtualToHack.keys -> popSegment(segment, index)
                    "temp" -> popTemp(index)
                    "pointer" -> popPointer(index)
                    "static" -> popStatic(index)
                    else -> throw IllegalArgumentException("No such command!")
                }
            } else {
                when (segment) {
                    in segmentsVirtualToHack.keys -> pushSegment(segment, index)
                    "constant" -> pushConstant(index)
                    "temp" -> pushTemp(index)
                    "pointer" -> pushPointer(index)
                    "static" -> pushStatic(index)
                    else -> throw IllegalArgumentException("No such command!")
                }
            }
        currentFile.appendText(codeToWrite)
    }

    fun close() {
        val loopAtTheEnd = """
(THE_END)
@THE_END
1;JMP
        """.trimIndent()
        currentFile.appendText(loopAtTheEnd)
    }

    private val decrementSP = """        
@SP
M=M-1

""".trimIndent()

    private val incrementSP = """        
@SP
M=M+1

""".trimIndent()

    private val popStackToR14 = """
@SP
A=M
D=M
@R14
M=D 

""".trimIndent()

    private val popStackToR13 = """
@SP
A=M
D=M
@R13
M=D 

""".trimIndent()

    private val popStackToD = """
$decrementSP
@SP
A=M
D=M 

""".trimIndent()

    private val pushStackFromD = """
@SP
A=M
M=D
$incrementSP

""".trimIndent()

    private fun addSubAndOr(sign: String): String {
        return """
$decrementSP
$popStackToR14 
$decrementSP 
$popStackToR13 

@R13
D=M
@R14
D=D${sign}M
$pushStackFromD

""".trimIndent()
    }

    private fun negNot(sign: String): String {
        return """
$decrementSP
@SP
A=M
M=${sign}M 
$incrementSP

""".trimIndent()
    }

    private fun equalGreaterLess(operand: String): String {
        val jmp = when (operand) {
            "=" -> "JEQ"
            "<" -> "JLT"
            else -> "JGT"
        }

        return """
$decrementSP
$popStackToR14
$decrementSP
$popStackToR13

@R14
D=D-M

@SATISFIED_$counter
D=D; $jmp

@SP
A=M
M=0

@END_$counter
0; JMP

(SATISFIED_$counter)
@SP
A=M
M=-1

(END_$counter)
$incrementSP

""".trimIndent()

    }

    private fun pushSegment(segment: String, index: Int): String {
        val seg = segmentsVirtualToHack[segment]

        return """
// push $segment $index
@${index}
D=A;

@${seg}
D=D+M;

A=D
D=M

$pushStackFromD

""".trimIndent()

    }

    private fun popSegment(segment: String, index: Int): String {
        val seg = segmentsVirtualToHack[segment]

        return """
// pop $segment $index
@$index
D=A

@$seg
D=D+M

@R13
M=D

$popStackToD 

@R13
A=M
M=D

""".trimIndent()

    }

    private fun pushTemp(index: Int): String {
        return """
// push temp $index
@R${5 + index}
D=M

$pushStackFromD

""".trimIndent()
    }

    private fun popTemp(index: Int): String {
        return """
// pop temp $index
$popStackToD

@R${5 + index}
M=D

""".trimIndent()
    }

    private fun pushPointer(index: Int): String {
        // OR should I throw an error?
        val seg = if (index == 0) "THIS" else "THAT"

        return """
// push pointer $index
@$seg
D=M

$pushStackFromD

""".trimIndent()


    }

    private fun popPointer(index: Int): String {
        // OR should I throw an error?
        val seg = if (index == 0) "THIS" else "THAT"

        return """
// pop pointer $index
$popStackToD
@$seg 
M=D

""".trimIndent()
    }

    private fun pushStatic(index: Int): String {
        val xxx = currentFile.name.substringBeforeLast(".vm")

        return """
// push static $index
@$xxx.$index
D=M

$pushStackFromD

""".trimIndent()
    }

    private fun popStatic(index: Int): String {
        val xxx = currentFile.name.substringBeforeLast(".vm")

        return """
// pop static $index
$popStackToD

@$xxx.$index
M=D

""".trimIndent()
    }

    private fun pushConstant(constant: Int): String {
        return """
// push constant $constant
@$constant
D=A

$pushStackFromD

""".trimIndent()
    }

    companion object {
        private val segmentsVirtualToHack =
            mapOf("local" to "LCL", "argument" to "ARG", "this" to "THIS", "that" to "THAT")
    }

}