package pt.isel

import java.io.Reader
import java.time.LocalDate

class Student @JvmOverloads constructor (
    val name: String,
    val nr: Int,
    @YamlArg("city of birth")
    val from: String,
    @YamlConvert(YamlToDate::class)
    val birth: LocalDate? = null,
    val address: Address? = null,
    val grades: List<Grade> = emptyList()
)


class YamlToDate : YamlConverter<LocalDate> {
    override fun convertToObject(input: String): LocalDate {
        return LocalDate.parse(input)
    }
}