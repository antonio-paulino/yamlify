package pt.isel

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class YamlParserReflectTest {

    @Test fun parseStudentWithMissingProperties() {
        val yaml = """
                name: Maria Candida
                from: Oleiros"""
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }
    @Test fun parseStudent() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros"""
        val st = YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        assertEquals("Maria Candida", st.name)
        assertEquals(873435, st.nr)
        assertEquals("Oleiros", st.from)
    }
    @Test fun parseStudentWithAddress() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                address:
                  street: Rua Rosa
                  nr: 78
                  city: Lisbon
                from: Oleiros"""
        val st = YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        assertEquals("Maria Candida", st.name)
        assertEquals(873435, st.nr)
        assertEquals("Oleiros", st.from)
        assertEquals("Rua Rosa", st.address?.street)
        assertEquals(78, st.address?.nr)
        assertEquals("Lisbon", st.address?.city)
    }

    @Test fun parseSequenceOfStrings() {
        val yaml = """
            - Ola
            - Maria Carmen
            - Lisboa Capital
        """
        val seq = YamlParserReflect.yamlParser(String::class)
            .parseList(yaml.reader())
            .iterator()
        assertEquals("Ola", seq.next())
        assertEquals("Maria Carmen", seq.next())
        assertEquals("Lisboa Capital", seq.next())
        assertFalse { seq.hasNext() }
    }

    @Test fun parseSequenceOfInts() {
        val yaml = """
            - 1
            - 2
            - 3
        """
        val seq = YamlParserReflect.yamlParser(Int::class)
            .parseList(yaml.reader())
            .iterator()
        assertEquals(1, seq.next())
        assertEquals(2, seq.next())
        assertEquals(3, seq.next())
        assertFalse { seq.hasNext() }
    }
    @Test fun parseSequenceOfStudents(){
        val yaml = """
            -
              name: Maria Candida
              nr: 873435
              from: Oleiros
            - 
              name: Jose Carioca
              nr: 1214398
              from: Tamega
        """
        val seq = YamlParserReflect.yamlParser(Student::class)
            .parseList(yaml.reader())
            .iterator()
        val st1 = seq.next()
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        val st2 = seq.next()
        assertEquals("Jose Carioca", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        assertFalse { seq.hasNext() }
    }
    @Test fun parseSequenceOfStudentsWithAddresses() {
        val yaml = """
            -
              name: Maria Candida
              nr: 873435
              address:
                street: Rua Rosa
                nr: 78
                city: Lisbon
              from: Oleiros
            - 
              name: Jose Carioca
              nr: 1214398
              address:
                street: Rua Azul
                nr: 12
                city: Porto
              from: Tamega
        """
        val seq = YamlParserReflect.yamlParser(Student::class)
            .parseList(yaml.reader())
            .iterator()
        val st1 = seq.next()
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        assertEquals("Rua Rosa", st1.address?.street)
        assertEquals(78, st1.address?.nr)
        assertEquals("Lisbon", st1.address?.city)
        val st2 = seq.next()
        assertEquals("Jose Carioca", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        assertEquals("Rua Azul", st2.address?.street)
        assertEquals(12, st2.address?.nr)
        assertEquals("Porto", st2.address?.city)
        assertFalse { seq.hasNext() }
    }
    @Test fun parseSequenceOfStudentsWithAddressesAndGrades() {
        val seq = YamlParserReflect.yamlParser(Student::class)
            .parseList(yamlSequenceOfStudents.reader())
            .iterator()
        assertStudentsInSequence(seq)
    }
    @Test fun parseClassroom() {
        val yaml = """
          id: i45
          students: $yamlSequenceOfStudents
        """.trimIndent()
        val cr = YamlParserReflect.yamlParser(Classroom::class)
            .parseObject(yaml.reader())
        assertEquals("i45", cr.id)
        assertStudentsInSequence(cr.students.iterator())
    }
    private fun assertStudentsInSequence(seq: Iterator<Student>) {
        val st1 = seq.next()
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        assertEquals("Rua Rosa", st1.address?.street)
        assertEquals(78, st1.address?.nr)
        assertEquals("Lisbon", st1.address?.city)
        val grades1 = st1.grades.iterator()
        val g1 = grades1.next()
        assertEquals("LAE", g1.subject)
        assertEquals(18, g1.classification)
        val g2 = grades1.next()
        assertEquals("PDM", g2.subject)
        assertEquals(15, g2.classification)
        val g3 = grades1.next()
        assertEquals("PC", g3.subject)
        assertEquals(19, g3.classification)
        assertFalse { grades1.hasNext() }
        val st2 = seq.next()
        assertEquals("Jose Carioca", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        assertEquals("Rua Azul", st2.address?.street)
        assertEquals(12, st2.address?.nr)
        assertEquals("Porto", st2.address?.city)
        val grades2 = st2.grades.iterator()
        val g4 = grades2.next()
        assertEquals("TDS", g4.subject)
        assertEquals(20, g4.classification)
        val g5 = grades2.next()
        assertEquals("LAE", g5.subject)
        assertEquals(18, g5.classification)
        assertFalse { grades2.hasNext() }
        assertFalse { seq.hasNext() }
    }

    @Test
    fun parseScalarListObject() {
        val yaml = """
            list:
              - 1
              - 2
              - 3
        """.trimIndent()
        val list = YamlParserReflect.yamlParser(IntList::class)
            .parseObject(yaml.reader())
            .list
        assertEquals(1, list[0])
        assertEquals(2, list[1])
        assertEquals(3, list[2])
    }

    @Test
    fun `parse student incorrect indentation at address object`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                address:
                  street: Rua Rosa
                  nr: 78
                   city: Lisbon
                from: Oleiros"""
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun `parse student incorrect indentation at student object`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
              address:
                  street: Rua Rosa
                  nr: 78
                  city: Lisbon
                from: Oleiros""".trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun `parse Int list incorrect indentation at scalar sequence`() {
        val yaml = """
             - 1
             - 2
              - 3
        """.trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Int::class).parseList(yaml.reader())
        }
    }

    @Test
    fun `parse List of List of str`() {
        val yaml = """
            - 
              - 1
              - 2
              - 3
            - 
              - 4
              - 5
              - 6
        """.trimIndent()
        val seq = YamlParserReflect.yamlParser(String::class)
            .parseList(yaml.reader()) as List<List<String>>
        assertEquals("1", seq[0][0])
        assertEquals("2", seq[0][1])
        assertEquals("3", seq[0][2])
        assertEquals("4", seq[1][0])
        assertEquals("5", seq[1][1])
        assertEquals("6", seq[1][2])
    }

    @Test
    fun `parse intListList`() {
        val yaml = """
         list:
           - 
             - 1
             - 2
             - 3
           - 
             - 4
             - 5
             - 6
            
        """.trimIndent()
        val seq = YamlParserReflect.yamlParser(IntListList::class)
            .parseObject(yaml.reader())
            .list
        assertEquals(1, seq[0][0])
        assertEquals(2, seq[0][1])
        assertEquals(3, seq[0][2])
        assertEquals(4, seq[1][0])
        assertEquals(5, seq[1][1])
        assertEquals(6, seq[1][2])
    }

    @Test
    fun `parse List of List of List of int`() {
        val yaml = """
            - 
              - 
                - 1
                - 2
                - 3
              - 
                - 4
                - 5
                - 6
            - 
              - 
                - 7
                - 8
                - 9
              - 
                - 10
                - 11
                - 12
        """.trimIndent()
        val seq = YamlParserReflect.yamlParser(Int::class)
            .parseList(yaml.reader()) as List<List<List<Int>>>
        assertEquals(1, seq[0][0][0])
        assertEquals(2, seq[0][0][1])
        assertEquals(3, seq[0][0][2])
        assertEquals(4, seq[0][1][0])
        assertEquals(5, seq[0][1][1])
        assertEquals(6, seq[0][1][2])
        assertEquals(7, seq[1][0][0])
        assertEquals(8, seq[1][0][1])
        assertEquals(9, seq[1][0][2])
        assertEquals(10, seq[1][1][0])
        assertEquals(11, seq[1][1][1])
        assertEquals(12, seq[1][1][2])
    }

    @Test
    fun `test parse student with duplicate properties`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros
                from: Lisboa
            """.trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun `test parse student with duplicate properties in address`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros
                address:
                  street: Rua Rosa
                  nr: 78
                  city: Lisbon
                  city: Porto
            """.trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }


    @Test
    fun `test parse student with renamed properties duplicated`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros
                city of birth: Lisboa
            """.trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun `test parse student with renamed from to city`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                city: Lisboa
            """.trimIndent()
        val st = YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        assertEquals("Maria Candida", st.name)
        assertEquals(873435, st.nr)
        assertEquals("Lisboa", st.from)
    }

    @Test
    fun `test parse student with renamed from to city of birth`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                city of birth: Lisboa
            """.trimIndent()
        val st = YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        assertEquals("Maria Candida", st.name)
        assertEquals(873435, st.nr)
        assertEquals("Lisboa", st.from)
    }

    @Test
    fun `test parse sequence of students with renamed props`() {
        val yaml = """
            -
              name: Maria Candida
              nr: 873435
              city: Oleiros
            - 
              name: Jose Carioca
              nr: 1214398
              city of birth: Tamega
        """
        val seq = YamlParserReflect.yamlParser(Student::class)
            .parseList(yaml.reader())
            .iterator()
        val st1 = seq.next()
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        val st2 = seq.next()
        assertEquals("Jose Carioca", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        assertFalse { seq.hasNext() }
    }

    @Test
    fun `test parse classroom renamed props`() {
        val yaml = """
          classroom_id: i45
          class_students: $yamlSequenceOfStudents
        """.trimIndent()
        val cr = YamlParserReflect.yamlParser(Classroom::class)
            .parseObject(yaml.reader())
        assertEquals("i45", cr.id)
        assertStudentsInSequence(cr.students.iterator())
    }

}



const val yamlSequenceOfStudents = """
            -
              name: Maria Candida
              nr: 873435
              address:
                street: Rua Rosa
                nr: 78
                city: Lisbon
              from: Oleiros
              grades:
                - 
                  subject: LAE
                  classification: 18
                -
                  subject: PDM
                  classification: 15
                -
                  subject: PC
                  classification: 19
            - 
              name: Jose Carioca
              nr: 1214398
              address:
                street: Rua Azul
                nr: 12
                city: Porto
              from: Tamega
              grades:
                -
                  subject: TDS
                  classification: 20
                - 
                  subject: LAE
                  classification: 18
        """

