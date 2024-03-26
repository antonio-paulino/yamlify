package pt.isel

class Student @JvmOverloads constructor (
    val name: String,
    val nr: Int,
    @YamlArg("city of birth")
    @YamlArg("city")
    val from: String,
    val address: Address? = null,
    val grades: List<Grade> = emptyList()
)