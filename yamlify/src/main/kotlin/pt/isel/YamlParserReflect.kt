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

        val constructor = type.constructors.first()

        if (constructor.parameters.isEmpty()) {
            return castValueToType(args.values.first(), type.starProjectedType) as T
        }

        val properties = args.keys.map { prop ->
            prop to matchParameter(prop, constructor.parameters)
        }

        val duplicateProps = properties.groupBy { it.second }.mapNotNull {
            if (it.value.size > 1) it
            else null
        }

        if (duplicateProps.isNotEmpty()) {
            val names = duplicateProps.map { prop -> prop.key!!.name to prop.value.map { it.first } }
            throw IllegalArgumentException("Duplicate properties: $names")
        }

        val constructorArgs = properties.mapNotNull { (srcProp, destProp) ->
            destProp?.let { destProp ->
                val value = args[srcProp]
                val type = destProp.type
                val castedValue = value?.let { castValueToType(it, type) }
                destProp to castedValue
            }
        }
        return constructor.callBy(constructorArgs.toMap())
    }
    private fun matchParameter(
        srcProp: String,
        ctorParameters: List<KParameter>) : KParameter?{
        return ctorParameters.firstOrNull { arg ->
            srcProp == arg.name || arg.annotations.any { it is YamlArg && it.yamlName == srcProp }
        }
    }
    private fun castValueToType(value: Any, type: KType): Any {
        val arg = value as? String
        return when (type.classifier) {
            String::class -> arg!!
            Int::class -> arg!!.toInt()
            Long::class -> arg!!.toLong()
            List::class -> getIterableValue(value, type)
            Set::class -> getIterableValue(value, type).toSet()
            Array::class -> getIterableValue(value, type).toTypedArray()
            Sequence::class -> getIterableValue(value, type).asSequence()
            ArrayList::class -> ArrayList(getIterableValue(value, type))
            else -> yamlParser(type.classifier as KClass<*>).newInstance(value as Map<String, Any>)
        }
    }
    private fun getIterableValue(value: Any, type: KType): List<Any> {

        val list = value as List<*>

        return list.map {
            if (it is List<*>)
                getIterableValue(it, type.arguments.first().type!!)
            else
                yamlParser(type.arguments.first().type!!.classifier as KClass<*>)
                    .newInstance((it as Map<String, Any>))
        }

    }

}
