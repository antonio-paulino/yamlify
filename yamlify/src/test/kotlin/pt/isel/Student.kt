package pt.isel

import java.io.Reader
import java.time.LocalDate

class Student @JvmOverloads constructor (
    val name: String,
    val nr: Int,
    @YamlArg("city of birth")
    @YamlArg("city")
    val from: String,
    @YamlConvert(YamlToDate::class)
    val birth: LocalDate? = null,
    val address: Address? = null,
    val grades: List<Grade> = emptyList()
)

object YamlToDate : YamlConverter<LocalDate> {
    override fun convertToObject(input: String): LocalDate {
        return LocalDate.parse(input)
    }
}