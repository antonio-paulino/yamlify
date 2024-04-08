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


    // Parse the yaml text into an object
    final override fun parseObject(yaml: Reader): T = newInstance(getObjectValues(yaml.useLines { it.joinToString("\n") }))

    // Parse the yaml text into a list of objects
    final override fun parseList(yaml: Reader): List<T> = createObjectsFromList(getListValues(yaml.useLines { it.joinToString("\n") }))

    // Creates a list of defined objects from a list of objects
    private fun createObjectsFromList(objectsList: List<Any>): List<T> {
        // Create a list to hold the objects
        val resultList = mutableListOf<T>()
        // Iterate over the list of objects
        objectsList.forEach {
            // Check if the object is a map
            if (it is Map<*, *>) {
                // If it is a map, create a new instance of the object
                resultList.add(newInstance(it as Map<String, Any>))
            } else if (it is List<*>) {
                // If it is a list, execute the function recursively
                // When it is a nested list
                resultList.add(createObjectsFromList(it as List<Any>) as T)
            }
        }
        return resultList
    }

    // Get the list of objects from yaml text
    private fun getListValues(yaml: String): List<Any> {
        // Read the text from the reader
        // this will return a string with the yaml content
        // Get the list of objects from the yaml text
        val yamlObjects = getObjectList(yaml)
        // Iterate over the list of objects
        return yamlObjects.map {
            // Check if the object is a list or a single object
            if (isList(it))
                // If it is a list, recursively call getListValues
                // to handle the nested list
                getListValues(it) as T
            else
                // If it is a single object, call getObjectValues
                getObjectValues(it) as T
        }
    }

    private fun isList(yaml: String): Boolean {
        val lines = yaml.split("\n").dropWhile { it.isBlank() }
        val objIndentation = getIndentation(lines.first())
        return lines.count { it[objIndentation] == '-' } > 1
    }

    // Get the values of the object from the yaml text
    private fun getObjectValues (yaml: String): Map<String, Any> {

        // Read the text from the reader
        // and clean the empty lines
        val lines = yaml.split("\n").dropWhile { it.isBlank() }
        // Create a mutable map to hold the results
        val map = mutableMapOf<String, Any>()
        // index to iterate over the lines
        var i = 0

        // Get the indentation of the object
        // In other words, the number of spaces before the first key
        val objIndentation = getIndentation(lines.first())

        // Iterate over the lines
        while (i < lines.size) {

            // Get the current line
            val line = lines[i++]

            // Check if the line is blank
            // If it is, continue to the next line
            if (line.isBlank()) continue

            // Get the indentation of the current line
            val indentation = getIndentation(line)

            // Check if the indentation is valid
            if (indentation != objIndentation) throw IllegalArgumentException("Invalid indentation at: ${line.trim()}")

            // Split the line by the colon
            // This returns an array with the key and the value
            val parts = line.split(":").map { it.trim() }.filter { it.isNotBlank() }

            when {
                // When the map already contains the key
                // Throw an exception due to duplicate keys
                map.containsKey(parts[0]) -> throw IllegalArgumentException("Duplicate key ${parts[0]} for ${type.simpleName}")
                // When the line has two parts (key and value)
                // Add the key and the value to the map
                parts.size == 2 -> map[parts[0]] = parts[1]
                // When the line is a scalar
                // Add the value to the map
                isScalar(line) -> map[parts[0]] = line.split("-").last().trim()
                // When the line is a list
                else -> {
                    // Get the next line
                    val nextLine = lines[i]
                    // Get the lines that are indented
                    val indentedLines = getLinesSequence(lines, i, indentation)
                    // Save as the value of the key,
                    // The result of the recursive call to getObjectValues
                    map[parts[0]] = if (nextLine.contains("-")) {
                        // If the next line contains a dash, it is a list
                        getListValues(indentedLines.joinToString("\n"))
                    } else {
                        // If the next line does not contain a dash, it is an object
                        getObjectValues(indentedLines.joinToString("\n"))
                    }
                    // Increment the index by the number of indented lines to skip over them
                    i += indentedLines.size
                }
            }

        }
        return map
    }

    // Check if a line is a scalar
    private fun isScalar(line: String): Boolean {
        return line.filter { it != '-' }.isNotBlank() && line.contains("-")
    }

    // Get the lines that are indented
    private fun getLinesSequence(lines: List<String>, start: Int, indentation: Int): List<String> {
        val result = mutableListOf<String>()
        var i = start
        while (i < lines.size) {
            val line = lines[i++]
            if (line.isBlank()) continue
            if (getIndentation(line) <= indentation) break
            result.add(line)
        }
        return result
    }

    // Get the list of objects from the yaml text
    private fun getObjectList(yaml: String): List<String> {

        // Remove the empty lines from the yaml text
        val lines = yaml.split("\n").dropWhile { it.isBlank() }
        // Get the indentation of the object
        val objIndentation = getIndentation(lines.first())

        // Create a mutable list to hold the results
        val list = mutableListOf<String>()
        // Create a mutable list to hold the current lines
        val currLines = mutableListOf<String>()

        // Iterate over the lines
        lines.forEach { line ->
            // Check if the line is blank
            if (line.isNotBlank()) {
                // Get the indentation of the current line
                val lineIndentation = getIndentation(line)

                // Check if the indentation is valid
                // In other words, if the difference between the current indentation and the object indentation is even
                if ((lineIndentation - objIndentation) % 2 != 0)
                    // If it is not valid, throw an exception
                    throw IllegalArgumentException("Invalid indentation at $line")

                // Check for list item separators
                if (line[objIndentation] == '-') {
                    // If it is a scalar, add the line to the list
                    if (isScalar(line)) {
                        list.add(line)
                    // If it is not a scalar
                    // Add the current object lines to the list
                    } else {
                        list.add(currLines.joinToString("\n"))
                        currLines.clear()
                    }
                } else {
                    // If there is no list item separator
                    // Add the line to the current object lines
                    currLines.add(line)
                }
            }
        }

        // Add the last object to the list
        list.add(currLines.joinToString("\n"))
        // End by filtering any empty objects
        return list.filter { it.isNotBlank() }
    }

    private fun getIndentation(line: String): Int {
        var count = 0
        while (line[count] == ' ') count++
        return count
    }

}