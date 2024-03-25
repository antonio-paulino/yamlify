package pt.isel

import kotlin.reflect.*
import kotlin.reflect.full.memberProperties
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
            val scalarList = args["list"] as List<*>
            return castValueToType(scalarList.first()!!, type as KClassifier) as T
        }

        val properties = args.keys.map { prop ->
            prop to matchParameter(prop, constructor.parameters)
        }

        val constructorArgs = properties.mapNotNull { (srcProp, destProp) ->
            destProp?.let { destProp ->
                val value = args[srcProp]
                val type = destProp.type.classifier as KClassifier
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
            srcProp == arg.name
        }
    }

    private fun castValueToType(value: Any, type: KClassifier): Any {
       return when (type) {
            String::class -> value as String
            Int::class -> (value as String).toInt()
            List::class -> {
                val map = value as Map<*, *>
                val list = map["list"] as List<*>
                if (this.type == Student::class) {
                    list.map { yamlParser(Grade::class).newInstance(it as Map<String, Any>) }
                } else {
                    TODO() // Non implemented types
                }
            }
           Sequence::class -> {
               val map = value as Map<*, *>
               val list = map["list"] as List<*>
               if(this.type == Classroom::class) {
                   if (type == Student::class)
                       list.map { yamlParser(Grade::class).newInstance(it as Map<String, Any>) }.asSequence()
                   else
                       list.map { yamlParser(Student::class).newInstance(it as Map<String, Any>) }.asSequence()
               } else {
                   TODO() // Non implemented types
               }
           }
            else -> yamlParser(type as KClass<*>).newInstance(value as Map<String, Any>)
       }
    }



}
