package pt.isel

import java.io.Reader
import kotlin.reflect.KClass

abstract class AbstractYamlParser<T : Any>(private val type: KClass<T>) : YamlParser<T> {
    /**
     * Used to get a parser for other Type using this same parsing approach.
     */
    abstract fun <T : Any> yamlParser(type: KClass<T>) : AbstractYamlParser<T>
    /**
     * Creates a new instance of T through the first constructor
     * that has all the mandatory parameters in the map and optional parameters for the rest.
     */
    abstract fun newInstance(args: Map<String, Any>): T


    final override fun parseObject(yaml: Reader): T = newInstance(getObjectValues(yaml))

    final override fun parseList(yaml: Reader): List<T> {
        val yamlText = yaml.readText()
        val yamlObjects = getObjectList(yamlText)
        return yamlObjects.map { parseObject(it.reader()) }
    }
    private fun getObjectValues (yaml: Reader): Map<String, Any> {
        val lines = yaml.readLines()
        val map = mutableMapOf<String, Any>()
        var i = 0

        while (i < lines.size) {

            val line = lines[i++]

            if (line.isBlank()) continue

            val indentation = line.takeWhile { it == ' ' }.length
            val parts = line.trim().split(":").map { it.trim() }.filter { it.isNotBlank() }

            if (parts.size == 1) {
                val isSeparator = line.filter { it != '-' }.isBlank()
                if (isSeparator) { // Sequence of mappings
                    val indentedLines = getLinesSequence(lines, i, indentation)
                    val list = map.getOrPut("list") { mutableListOf<Map<String, Any>>() } as MutableList<Map<String, Any>>
                    list.add(getObjectValues(indentedLines.joinToString("\n").reader()))
                    i += indentedLines.size
                } else if (line.contains("-")) { // sequence of scalars
                    val value = line.split("-").last().trim()
                    val list = map.getOrPut("list") { mutableListOf<String>() } as MutableList<String>
                    list.add(value)
                    map["list"] = list
                }
                else { // object
                    val indentedLines = getLinesSequence(lines, i, indentation)
                    map[parts[0]] = getObjectValues(indentedLines.joinToString("\n").reader())
                    i += indentedLines.size
                }
            } else { // scalar to scalar
                map[parts[0]] = parts[1]
            }
        }

        return map
    }
    private fun getLinesSequence(lines: List<String>, start: Int, indentation: Int): List<String> {
        return lines.drop(start).takeWhile { line ->
            val lineIndentation = line.takeWhile { it == ' ' }.length
            line.isNotBlank() && lineIndentation > indentation
        }
    }
    private fun getObjectList(yaml: String): List<String> {

        val lines = yaml.lines().dropWhile { it.isBlank() }
        val indentation = lines.first().takeWhile { it == ' ' }.length

        val list = lines.takeWhile { it.contains("-") && it.filter { it != '-' }.isNotBlank() }.toMutableList()

        val currLines = mutableListOf<String>()

        lines.forEach { line ->
            if (line.length < indentation || line.isBlank()) return@forEach
            if (line[indentation] == '-') {
                list.add(currLines.joinToString("\n"))
                currLines.clear()
            } else {
                currLines.add(line)
            }
        }

        list.add(currLines.joinToString("\n"))
        return list.filter { it.isNotBlank() }
    }
}