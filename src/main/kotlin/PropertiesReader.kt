import java.util.*

// https://medium.com/@platky/very-simple-property-reading-in-kotlin-3265cb4382bf
abstract class PropertiesReader(fileName: String) {
    private val properties = Properties()

    init {
        val file = this::class.java.classLoader.getResourceAsStream(fileName)
        properties.load(file)
    }

    fun getProperty(key: String): String = properties.getProperty(key)
}

class Facebook4jProperties : PropertiesReader("facebook4j.properties")
class FacebookProperties : PropertiesReader("facebook.properties")

