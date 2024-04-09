package pt.isel

import kotlin.reflect.*
import kotlin.reflect.full.starProjectedType


/**
 * A YamlParser that uses reflection to parse objects.
 */
class YamlParserReflect<T : Any> private constructor(private val type: KClass<T>) : AbstractYamlParser<T>(type) {

    private val constructor = type.constructors.first()
    companion object {
        /**
         *Internal cache of YamlParserReflect instances.
         */
        private val yamlParsers: MutableMap<KClass<*>, YamlParserReflect<*>> = mutableMapOf()
        /**
         * Creates a YamlParser for the given type using reflection if it does not already exist.
         * Keep it in an internal cache of YamlParserReflect instances.
         */
        fun <T : Any> yamlParser(type: KClass<T>): AbstractYamlParser<T> {
            return yamlParsers.getOrPut(type) { YamlParserReflect(type) } as YamlParserReflect<T>
        }
    }
    /**
     * Used to get a parser for other Type using the same parsing approach.
     */
    override fun <T : Any> yamlParser(type: KClass<T>) = YamlParserReflect.yamlParser(type)
    /**
     * Creates a new instance of T through the first constructor
     * that has all the mandatory parameters in the map and optional parameters for the rest.
     */

    override fun newInstance(args: Map<String, Any>): T {
        // If the constructor has no parameters, return a new instance of the type
        if (constructor.parameters.isEmpty()) {
            return castValueToType(args.values.first(), type.starProjectedType) as T
        }

        // Map the properties to the constructor parameters
        val properties = mutableListOf<Pair<String,KParameter>>()
        args.keys.forEach { srcProp ->
            val param = matchParameter(srcProp, constructor.parameters)
            if (param != null) {
                if (properties.any { it.second == param }) {
                    throw IllegalArgumentException("Duplicate parameter: ${param.name}")
                }
                properties.add(Pair(srcProp, param))
            }
        }

        // Get the constructor arguments
        val constructorArgs = properties.associateBy({ it.second!! }, { (srcProp, destProp) ->
            destProp.run {
                val value = args[srcProp]

                if (destProp.annotations.any { it is YamlConvert }) {
                    val customParser = destProp.annotations.first { it is YamlConvert } as YamlConvert
                    val customParserInstance = customParser.parser.objectInstance
                    customParserInstance?.convertToObject(value.toString())
                } else {
                    value?.let { castValueToType(it, type) } // Cast the value to the suitable type
                }
            }
        })

        return constructor.callBy(constructorArgs)
    }

    // Get the first constructor parameter that matches the property
    private fun matchParameter(
        // The source property name
        srcProp: String,
        // The constructor parameters list
        ctorParameters: List<KParameter>) : KParameter?{
        // Return the first parameter that matches the property
        return ctorParameters.firstOrNull { arg ->
            // Check if the property name is equal to the parameter name
            // or if the property has a YamlArg annotation with the same name as the parameter
            srcProp == arg.name || arg.annotations.any { it is YamlArg && it.yamlName == srcProp }
        }
    }

    // Cast the given value to the suitable type
    private fun castValueToType(value: Any, type: KType): Any {
        val arg = value as? String
        return when (type.classifier) {
            String::class -> arg!!
            Char::class -> arg!!.first()
            Int::class -> arg!!.toInt()
            Long::class -> arg!!.toLong()
            Double::class -> arg!!.toDouble()
            Float::class -> arg!!.toFloat()
            Boolean::class -> arg!!.toBoolean()
            Byte::class -> arg!!.toByte()
            Short::class -> arg!!.toShort()
            UByte::class -> arg!!.toUByte()
            UShort::class -> arg!!.toUShort()
            UInt::class -> arg!!.toUInt()
            ULong::class -> arg!!.toULong()
            List::class -> getIterableValue(value, type)
            Set::class -> getIterableValue(value, type).toSet()
            Array::class -> getIterableValue(value, type).toTypedArray()
            Sequence::class -> getIterableValue(value, type).asSequence()
            ArrayList::class -> ArrayList(getIterableValue(value, type))
            HashSet::class -> HashSet(getIterableValue(value, type))
            Collection::class -> getIterableValue(value, type)
            else -> yamlParser(type.classifier as KClass<*>).newInstance(value as Map<String, Any>)
        }
    }
    private fun getIterableValue(value: Any, type: KType): List<Any> {

        // Cast the value to a List of Any
        val list = value as List<*>

        // Iterate over the list
        return list.map {
            // If the type is a list, recursively call getIterableValue to instance its elements
            if (it is List<*>)
                getIterableValue(it, type.arguments.first().type!!)
            else
                // Use the yamlParser to get the instances of the list element type
                yamlParser(type.arguments.first().type!!.classifier as KClass<*>)
                    .newInstance((it as Map<String, Any>))
        }

    }

}
