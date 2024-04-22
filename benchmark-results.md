# Yaml Parser Benchmarks

### How to run 

To execute the benchmark, run the following commands in the root of the repository:
 - `./gradlew jmhJar`
 - `java -jar yamlify-bench/build/libs/yamlify-bench-jmh.jar -i 10 -wi 4 -f 1 -r 2 -w 2 -tu ms`


### Yaml Parser Benchmarks


The Cojen and Reflect parsers are generic parsers that can be used to parse any domain class.

The Baseline parsers are specific to the domain class they are parsing. 

The following benchmarks show the average results for 10 iterations of 2 second runs for each parser implementation. The benchmark was run on a machine with a Ryzen 5 5600 CPU and 16GB 3200mhz RAM.

### Savings Account Benchmark

#### Savings Account Class Structure
 
The Savings Account class has the following structure:

```kotlin
class SavingsAccount(
    val accountCode: Short,
    val holderName: String,
    val balance: Long,
    val isActive: Boolean,
    val interestRate: Double,
    val transactionLimit: Int,
    val withdrawLimit: Int,
)
```

#### Benchmark Results and Analysis

| Parser   | Operations per millisecond | Relative Performance |
|----------|---------------------------|----------------------|
| Baseline | 532.249 ± 2.907            | 1.000                |
| Cojen    | 533.679 ± 1.817            | 1.003                |
| Reflect  | 310.881 ± 1.017            | 0.584                |

It can be observed that the Cojen parser's performance is within the margin of error of the baseline parser.
This stems from the fact that the Cojen parser dinamically generates code to instantiate the domain class at runtime
during its initial invocation. Once this code is generated, subsequent calls to the parser benefit from utilizing
the pre-generated code, eliminating the overhead of reflection. 

The Reflect parser, on the other hand, shows significantly slower performance compared to both the baseline and Cojen parsers.
Unlike the Cojen parser, the Reflect parser relies on reflection to instantiate the domain class every time it is called.
This repeated use of reflection results in a significant performance overhead, leading to the observed results.

### Student Benchmark

##### Student Class Structure
The Student class is a more complex class compared to the Savings Account class. It has the following structure:
```kotlin
class Student(
    val id: Int,
    val name: String,
    val birth: LocalDate,
    val address: Address,
    val grades: List<Grade>,
)
class Grade(
    val subject: String,
    val classification: Int
)
class Address(
    val street: String, 
    val number: Int, 
    val city: String
)
```
##### Benchmark Results and Analysis
| Parser   | Operations per millisecond | Relative Performance |
|----------|---------------------------|----------------------|
| Baseline | 300.814 ± 1.658            | 1.000                |
| Cojen    | 296.780 ± 1.013            | 0.987                |
| Reflect  | 171.673 ± 0.628            | 0.570                |

Once again, the Cojen parser's performance is close to the baseline parser's performance, with a slight decrease in relative performance compared to the Savings Account benchmark.

The Reflect parser's performance is again, significantly slower than the baseline and Cojen parsers, with a slight decrease in relative performance compared to the Savings Account benchmark. 

This slight decrease in relative performance for both the Cojen and Reflect parsers can be attributed to the increased complexity of the Student class compared to the Savings Account class. 
The Student class has a nested Address class property and a list of instances of the Grade class, which requires additional parser calls and instantiation steps.
These additional steps result in slight decrease in performance 
for both parsers, relative to the Baseline parser.