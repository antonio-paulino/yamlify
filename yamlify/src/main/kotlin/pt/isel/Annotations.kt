package pt.isel

@Repeatable
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class YamlArg(val yamlName: String)
