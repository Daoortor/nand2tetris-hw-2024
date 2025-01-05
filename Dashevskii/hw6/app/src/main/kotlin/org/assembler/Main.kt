package org.assembler

import java.io.File

fun List<String>.format(): List<String> = map {
        it.filter { !it.isWhitespace() }
    }.filter { !it.startsWith("//") && it.isNotEmpty() }

object Assembler {
    val predefinedSymbols = mapOf(
        "R0" to 0,
        "R1" to 1,
        "R2" to 2,
        "R3" to 3,
        "R4" to 4,
        "R5" to 5,
        "R6" to 6,
        "R7" to 7,
        "R8" to 8,
        "R9" to 9,
        "R10" to 10,
        "R11" to 11,
        "R12" to 12,
        "R13" to 13,
        "R14" to 14,
        "R15" to 15,
        "SCREEN" to 16384,
        "KBD" to 24576,
        "SP" to 0,
        "LCL" to 1,
        "ARG" to 2,
        "THIS" to 3,
        "THAT" to 4
    )

    val labelSymbols = mutableMapOf<String, Int>()
    val variableSymbols = mutableMapOf<String, Int>()
    var currentVariable = 16

    fun String.isLabel() = matches("\\(.*\\)".toRegex())

    fun translateA(line: String): String {
        line.drop(1).toUIntOrNull()?.let {
            return "0${it.toString(radix = 2).padStart(15, '0')}"
        }
        predefinedSymbols[line.drop(1)]?.let {
            return "0${it.toString(radix = 2).padStart(15, '0')}"
        }
        labelSymbols[line.drop(1)]?.let {
            return "0${it.toString(radix = 2).padStart(15, '0')}"
        }
        variableSymbols[line.drop(1)]?.let {
            return "0${it.toString(radix = 2).padStart(15, '0')}"
        }
        variableSymbols[line.drop(1)] = currentVariable
        return "0${currentVariable++.toString(radix = 2).padStart(15, '0')}"
    }

    fun translateDest(dest: String): String {
        return when(dest) {
            "null" -> "000"
            "M" -> "001"
            "D" -> "010"
            "MD" -> "011"
            "A" -> "100"
            "AM" -> "101"
            "AD" -> "110"
            "AMD" -> "111"
            else -> error("Invalid dest: $dest")
        }
    }

    fun translateComp(comp: String): String {
        return when(comp) {
            "0" -> "0101010"
            "1" -> "0111111"
            "-1" -> "0111010"
            "D" -> "0001100"
            "A" -> "0110000"
            "!D" -> "0001101"
            "!A" -> "0110001"
            "-D" -> "0001111"
            "-A" -> "0110011"
            "D+1" -> "0011111"
            "A+1" -> "0110111"
            "D-1" -> "0001110"
            "A-1" -> "0110010"
            "D+A" -> "0000010"
            "D-A" -> "0010011"
            "A-D" -> "0000111"
            "D&A" -> "0000000"
            "D|A" -> "0010101"
            "M" -> "1110000"
            "!M" -> "1110001"
            "-M" -> "1110011"
            "M+1" -> "1110111"
            "M-1" -> "1110010"
            "D+M" -> "1000010"
            "D-M" -> "1010011"
            "M-D" -> "1000111"
            "D&M" -> "1000000"
            "D|M" -> "1010101"
            else -> error("Invalid compute instruction: $comp")
        }
    }

    fun translateJump(jump: String): String {
        return when(jump) {
            "null" -> "000"
            "JGT" -> "001"
            "JEQ" -> "010"
            "JGE" -> "011"
            "JLT" -> "100"
            "JNE" -> "101"
            "JLE" -> "110"
            "JMP" -> "111"
            else -> error("Invalid jump instruction: $jump")
        }
    }

    fun translateC(line: String): String {
        val equalsIndex = line.indexOf('=')
        val semicolonIndex = line.indexOf(';')
        val dest = if(equalsIndex != -1) line.substring(0, equalsIndex) else "null"
        val comp = line.substring(equalsIndex + 1, if(semicolonIndex == -1) line.length else semicolonIndex)
        val jump = if(semicolonIndex != -1) line.substring(semicolonIndex + 1) else "null"
        return "111" + translateComp(comp) + translateDest(dest) + translateJump(jump)
    }

    fun translateLine(line: String): String {
        return when {
            line.firstOrNull() == '@' -> translateA(line)
            else -> translateC(line)
        }
    }
    fun initSymbols(lines: List<String>) {
        var index = 0
        lines.forEach {
            if(it.isLabel()) {
                labelSymbols[it.drop(1).dropLast(1)] = index
            } else index ++
        }
    }

    fun translate(lines: List<String>): List<String> {
        initSymbols(lines)
        return lines.filter{ !it.isLabel() }.map {
            translateLine(it)
        }
    }
}

fun main(args: Array<String>) {
    var inFilename = args.firstOrNull() ?: error("No arguments specified.")
    var outFilename = inFilename.substringBeforeLast('.') + ".hack"

    var inFile = File(inFilename)
    if (!inFile.exists()) {
        error("File $outFilename does not exist.")
    }
    if (!inFile.canRead()) {
        error("File $outFilename is not readable.")
    }
    var outFile = File(outFilename)
    if (!outFile.canWrite()) {
        error("File $outFilename is not writable.")
    }
    System.err.println("Writing the result to $outFilename ...")
    var inLines = inFile.readLines()
    outFile.writeText(Assembler.translate(inLines.format()).joinToString(System.lineSeparator()))
}
