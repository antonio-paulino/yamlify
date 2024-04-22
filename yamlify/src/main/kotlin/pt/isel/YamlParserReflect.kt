package pt.isel

import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.starProjectedType


/**
 * A YamlParser that uses reflection to parse objects.
 */
class YamlParserReflect<T : Any> private constructor(private val type: KClass<T>) : AbstractYamlParser<T>(type) {

    private val constructor = type.constructors.first()

    private val constructorProps : MutableMap<String, KClass<*>> = mutableMapOf()

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
            return castSimpleType(args.values.first(), type.starProjectedType) as T
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
        val constructorArgs = properties.associateBy({ it.second }, { (srcProp, destProp) ->
            destProp.run {
                val value = args[srcProp]

                if (destProp.annotations.any { it is YamlConvert }) {
                    val customParser = destProp.annotations.first { it is YamlConvert } as YamlConvert
                    val customParserInstance = customParser.parser.createInstance()
                    customParserInstance.convertToObject(value.toString())
                } else {
                    value?.let { castValueToParamType(it, destProp) }
                }
            }
        })

        return constructor.callBy(constructorArgs)
    }

    private fun matchParameter(srcProp: String, ctorParameters: List<KParameter>) : KParameter? {
        return ctorParameters.firstOrNull { arg ->
            srcProp == arg.name || arg.annotations.any { it is YamlArg && it.yamlName == srcProp }
        }
    }

    private fun getClassifier(name: String, type: KType): KClass<*> {
        return constructorProps.getOrPut(name) {
            type.classifier as KClass<*>
        }
    }

    // Casts a simple type to the desired type
    // for when the type has no constructor parameters
    private fun castSimpleType(value: Any, type: KType): Any? {
        val classifier = getClassifier(this.type.simpleName!!, type)
        return when (classifier) {
            String::class -> value.toString()
            Char::class -> value.toString().first()
            Int::class -> value.toString().toInt()
            Long::class -> value.toString().toLong()
            Double::class -> value.toString().toDouble()
            Float::class -> value.toString().toFloat()
            Boolean::class -> value.toString().toBoolean()
            Byte::class -> value.toString().toByte()
            Short::class -> value.toString().toShort()
            UByte::class -> value.toString().toUByte()
            UShort::class -> value.toString().toUShort()
            UInt::class -> value.toString().toUInt()
            ULong::class -> value.toString().toULong()
            else -> null
        }
    }

    // Casts a value to the parameter type
    // Used when the type has a constructor with parameters
    private fun castValueToParamType(value: Any, parameter: KParameter): Any {
        val paramType = parameter.type
        val arg = value as? String
        val classifier = getClassifier(parameter.name!!, paramType)
        return when (classifier) {
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
            List::class -> getIterableValue(value, paramType)
            Set::class -> getIterableValue(value, paramType).toSet()
            Array::class -> getIterableValue(value, paramType).toTypedArray()
            Sequence::class -> getIterableValue(value, paramType).asSequence()
            ArrayList::class -> ArrayList(getIterableValue(value, paramType))
            HashSet::class -> HashSet(getIterableValue(value, paramType))
            Collection::class -> getIterableValue(value, paramType)
            else -> yamlParser(classifier).newInstance(value as Map<String, Any>)
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