package pt.isel

import kotlin.reflect.*
import kotlin.reflect.full.starProjectedType


/**
 * A YamlParser that uses reflection to parse objects.
 */
class YamlParserReflect<T : Any>(private val type: KClass<T>) : AbstractYamlParser<T>(type) {

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

        // Get the first constructor of the type (the primary constructor)
        val constructor = type.constructors.first()

        // If the constructor has no parameters, return a new instance of the type
        if (constructor.parameters.isEmpty()) {
            return castValueToType(args.values.first(), type.starProjectedType) as T
        }

        // Map the properties to the constructor parameters
        val properties = args.keys.map { prop ->
            prop to matchParameter(prop, constructor.parameters)
        }

        // Check for duplicate properties
        val duplicateProps = properties.groupBy { it.second }.mapNotNull {
            // If it's (the property) value has more than one element, it is a duplicate
            if (it.value.size > 1) it
            else null
        }

        // If there are duplicate properties, throw an exception with the names of the properties
        if (duplicateProps.isNotEmpty()) {
            val names = duplicateProps.map { prop -> prop.key!!.name to prop.value.map { it.first } }
            throw IllegalArgumentException("Duplicate properties: $names")
        }

        // Get the constructor arguments
        val constructorArgs = properties.mapNotNull { (srcProp, destProp) ->
            destProp?.let { destProp ->
                val value = args[srcProp] // Get the value of the property
                val type = destProp.type // Get the type of the property
                val castedValue = value?.let { castValueToType(it, type) } // Cast the value to the suitable type
                destProp to castedValue
            }
        }
        // Return a new instance of the type using the constructor with the arguments
        return constructor.callBy(constructorArgs.toMap())
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
            String::class -> arg!!  // If the type is a string, return the value as is
            Int::class -> arg!!.toInt() // If the type is an int, return the value as an int
            Long::class -> arg!!.toLong()   // If the type is a long, return the value as a long
            List::class -> getIterableValue(value, type)    // If the type is a list, use getIterableValue to get the list
            Set::class -> getIterableValue(value, type).toSet() // If the type is a set, use getIterableValue to get the set
            Array::class -> getIterableValue(value, type).toTypedArray()    // If the type is an array, use getIterableValue to get the array
            Sequence::class -> getIterableValue(value, type).asSequence()   // If the type is a sequence, use getIterableValue to get the sequence
            ArrayList::class -> ArrayList(getIterableValue(value, type))    // If the type is an ArrayList, use getIterableValue to get the list and create a new ArrayList
            else -> yamlParser(type.classifier as KClass<*>).newInstance(value as Map<String, Any>) // If the type is a custom type, use the yamlParser to get the instance
        }
    }
    private fun getIterableValue(value: Any, type: KType): List<Any> {

        // Cast the value to a List of Any
        val list = value as List<*>

        // Iterate over the list
        return list.map {
            // If the type is a list, use getIterableValue to get the list
            if (it is List<*>)
                getIterableValue(it, type.arguments.first().type!!)
            // If the type is a not a list
            else
                // If the type is a custom type, use the yamlParser to get the instance
                yamlParser(type.arguments.first().type!!.classifier as KClass<*>)
                    .newInstance((it as Map<String, Any>))
        }

    }

}
