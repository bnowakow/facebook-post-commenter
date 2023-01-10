import java.util.*


abstract class PropertiesReader(private val fileName: String) {
    private val properties = Properties()

    init {
        val file = this::class.java.classLoader.getResourceAsStream(fileName)
        properties.load(file)
    }

    fun getProperty(key: String): String = properties.getProperty(key)
}

class Facebook4jProperties(): PropertiesReader("facebook4j.properties")

