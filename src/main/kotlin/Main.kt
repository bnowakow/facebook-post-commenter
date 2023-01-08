import facebook4j.Facebook
import facebook4j.FacebookFactory

fun main(args: Array<String>) {

    val facebook: Facebook = FacebookFactory().getInstance()
    val posts = facebook.getPosts("105161449087504")

    println("Posts: ${posts.joinToString()}")
}