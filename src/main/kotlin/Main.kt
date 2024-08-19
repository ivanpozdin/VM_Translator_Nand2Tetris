package org.example

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

fun getAllVMFilesFromDirectory(input: Path): List<Path> {
    return input.toFile().walk().filter { it.name.substringAfterLast(".") == "vm" }.map { it.toPath() }.toList()

}

fun main() {
    println("Write the full path to the file:")
    val source = Path(readln())

    val files = getAllVMFilesFromDirectory(source)
    val codeWriter = CodeWriter()

    for (input in files) {
        val parser = Parser(input)

        val output = Path(input.parent.toString(), input.nameWithoutExtension + ".asm")
        codeWriter.setFileName(output.toString())

        while (parser.hasMoreCommands) {
            parser.advance()
            if (parser.commandType == Command.ARITHMETIC)
                codeWriter.writeArithmetic(parser.arg1)

            if (parser.commandType in listOf(Command.PUSH, Command.POP))
                codeWriter.writePushPop(parser.commandType, parser.arg1, parser.arg2.toInt())
        }
        codeWriter.close()
    }
}