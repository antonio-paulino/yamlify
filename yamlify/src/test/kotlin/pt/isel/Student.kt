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
    val birth: LocalDate,
    val address: Address? = null,
    val grades: List<Grade> = emptyList()
)

object YamlToDate : YamlParser<LocalDate> {
    override fun parseObject(yaml: Reader): LocalDate {
        return LocalDate.parse(yaml.readText())
    }

    override fun parseList(yaml: Reader): List<LocalDate> {
        return yaml.readText().lines().map { LocalDate.parse(it) }
    }
}