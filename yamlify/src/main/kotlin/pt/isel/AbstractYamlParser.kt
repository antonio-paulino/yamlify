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

    final override fun parseList(yaml: Reader): List<T> = createObjectsFromList(getListValues(yaml))

    private fun createObjectsFromList(objectsList: List<Any>): List<T> {
        val resultList = mutableListOf<T>()
        objectsList.forEach {
            if (it is Map<*, *>) {
                resultList.add(newInstance(it as Map<String, Any>))
            } else if (it is List<*>) {
                resultList.add(createObjectsFromList(it as List<Any>) as T)
            }
        }
        return resultList
    }

    private fun getListValues(yaml: Reader): List<Any> {
        val yamlText = yaml.readText()
        val yamlObjects = getObjectList(yamlText)
        var resultList = mutableListOf<T>()
        yamlObjects.forEach {
            if (getObjectList(it).size > 1) //check for nested lists
                resultList.add(getListValues(it.reader()) as T)
            else
                resultList.add(getObjectValues(it.reader()) as T)
        }
        return resultList
    }

    private fun getObjectValues (yaml: Reader): Map<String, Any> {

        val lines = yaml.readLines().dropWhile { it.isBlank() }
        val map = mutableMapOf<String, Any>()
        var i = 0

        val objIndentation = lines.first().takeWhile { it == ' ' }.length

        while (i < lines.size) {

            var line = lines[i++]

            if (line.isBlank()) continue

            val indentation = line.takeWhile { it == ' ' }.length

            if (indentation != objIndentation) throw IllegalArgumentException("Invalid indentation at: ${line.trim()}")

            val parts = line.split(":").map { it.trim() }.filter { it.isNotBlank() }

            when {
                map.containsKey(parts[0]) -> throw IllegalArgumentException("Duplicate key ${parts[0]} for ${type.simpleName}")
                parts.size == 2 -> map[parts[0]] = parts[1]
                isScalar(line) -> map[parts[0]] = line.split("-").last().trim()
                else -> {
                    val nextLine = lines[i]
                    val indentedLines = getLinesSequence(lines, i, indentation)

                    map[parts[0]] = if (nextLine.contains("-")) {
                        getListValues(indentedLines.joinToString("\n").reader())
                    } else {
                        getObjectValues(indentedLines.joinToString("\n").reader())
                    }

                    i += indentedLines.size
                }
            }

        }


        return map
    }

    private fun isScalar(line: String): Boolean {
        return line.filter { it != '-' }.isNotBlank() && line.contains("-")
    }
    private fun getLinesSequence(lines: List<String>, start: Int, indentation: Int): List<String> {
        return lines.drop(start).takeWhile { line ->
            val lineIndentation = line.takeWhile { it == ' ' }.length
            lineIndentation > indentation
        }
    }
    private fun getObjectList(yaml: String): List<String> {

        val lines = yaml.lines().dropWhile { it.isBlank() }
        val objIndentation = lines.first().takeWhile { it == ' ' }.length

        val list = mutableListOf<String>()
        val currLines = mutableListOf<String>()

        lines.forEach { line ->
            if (line.isNotBlank()) {
                val lineIndentation = line.takeWhile { it == ' ' }.length

                if ((lineIndentation - objIndentation) % 2 != 0)
                    throw IllegalArgumentException("Invalid indentation at $line")

                if (line[objIndentation] == '-') {
                    if (isScalar(line)) {
                        list.add(line)
                    } else {
                        list.add(currLines.joinToString("\n"))
                        currLines.clear()
                    }
                } else {
                    currLines.add(line)
                }
            }
        }

        list.add(currLines.joinToString("\n"))
        return list.filter { it.isNotBlank() }
    }
}