import facebook4j.Comment
import facebook4j.Facebook
import facebook4j.FacebookFactory
import facebook4j.Post
import facebook4j.ResponseList

fun main(args: Array<String>) {

    val facebook: Facebook = FacebookFactory().getInstance()
    val posts: ResponseList<Post> = facebook.getPosts("105161449087504")

    for (post in posts) {
        val comments: ResponseList<Comment> = facebook.getPostComments(post.id)

        for (comment in comments) {
            println("comment: ${comment}")
        }
    }



}