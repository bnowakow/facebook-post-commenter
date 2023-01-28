import facebook4j.*
import mu.KotlinLogging
import pl.bnowakowski.facebook_commenter.FacebookPost


fun main() {

    val facebook: Facebook = FacebookFactory().instance
    val facebookReplies = FacebookReplies(facebook)
    val logger = KotlinLogging.logger {}
    val facebookProperties = FacebookProperties()
    val facebook4jProperties = Facebook4jProperties()

    // TODO automate inviting people liking page to join grou
    // TODO find notification with people sharing fanpage (orange flag icon in notification) and comment it

    facebook.extendTokenExpiration()

    val posts: ResponseList<Post> = facebook.getPosts("105161449087504") // Kuba
    logger.info("will be processing ${posts.size} fan page posts:")

    var facebookSharedPosts: FacebookSharedPosts? = null
    if (facebookProperties.getProperty("workaround-enabled") == "true") {
        facebookSharedPosts = FacebookSharedPosts()
        facebookSharedPosts.loginToFacebook()
        facebookSharedPosts.switchProfileToFanPage()
    }

    for (post in posts) {
        logger.info("in post [${FacebookPost.previewMessage(post)}...], ${post.id}")

        // comments under posts via API
        if (facebook4jProperties.getProperty("enabled") == "true") {
            logger.info("\tlooking into comments under post")
            facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(post)
        }

        // shared posts via API
        //  val sharedPosts = facebook.getSharedPosts(post.id) // API

        // shared posts using workaround
        if (facebookProperties.getProperty("workaround-enabled") == "true" &&
            facebookSharedPosts !== null) {
            logger.info("\tlooking into shared posts using workaround")
            facebookSharedPosts.openSharedPosts(post.id)
        }
    }

    // TODO figure out can we get id's of ad posts from API
    val adPostIds: List<String> = listOf(
        "105161449087504_pfbid02Qnx3ctSvN2Z2JJDEzp25kcdsLgNVSNtHV1bF57psQTR5zWHY6NgEExRnSxMBw6A9l",
        "105161449087504_pfbid0gywSSeZKvCFomR5dELyr2ULFpk35SLHAaE5USdiMeyWw4H6bi5yLBVrHnnVN4tuEl",
        "105161449087504_pfbid02QvbZbFUeYnwnktYc1Ryfi617mAMaJC6r655NxWENXF3VoqVRkE6DjhmdCrKZhoLQl",
        "105161449087504_pfbid0VSV5LB9fjJNpas2RyCRgfEjYdV8hbyZ24pnxCNtWKuJFCdHuAoSs2DfgLDheCrDtl",
        "105161449087504_pfbid02Z7mnDhokm7qsKJKNHdE6xWxtJJSx2CaVRrGsxa9kLLFWiGBnnVHCFf9QX6eVQ54Ml",
        "105161449087504_pfbid0V6B4jtcC67k8KtrL1UuFFexNVn7uNWr5psXiXnJSfxdqbLfuEs6Es3EragggRiVXl",
        "105161449087504_pfbid02kGQeR7Rvg73KXmW7PGRhWZWuTTHa9Vz3btLiDoZKg4qkvjGPUuD44RxAjTE4Uwjql",
        "105161449087504_pfbid0MAHt4ALYF5u5rmwRYNAjVq1ZHLQr7H9Cnvm52CL1Hp7mScWjRmNgKAMQn5TngvrJl",
        "105161449087504_pfbid02YmTmdRGH82g4ftA6GnNYRuuVxRU58qVnZYMHnqmybHuRS8Lm53v3AsEfvTsf8Wz4l",
        "105161449087504_1172046396767560",
        "105161449087504_pfbid0L8t5oum9D78tfeFybVqr8yDgU2WLAvUdMYznMa5n5cKHH4bvTKLNbkKYCxKJgNtHl",
        "105161449087504_pfbid02PYSh1VNnQ88X6U7mBTfQHvUqBW5ZbRiXZ9Di5WfzwLmVPEoAe7gPrjDmZ5YCATMHl",
        "105161449087504_pfbid02PrFDGzgFQ7iiMJ8tpEAtW56weVYahCBTZQeZfHPzfJo3ssJo5AoaGYSbtPaEDcyZl"
    )

    logger.info("will be processing ${adPostIds.size} ad posts:")

    for (adPostId in adPostIds) {
        val post = facebook.getPost(adPostId)
        logger.info("in post [${FacebookPost.previewMessage(post)}...], ${post.id}")

        // comments under ad posts via API
        if (facebook4jProperties.getProperty("enabled") == "true") {
            logger.info("\tlooking into comments under post")
            facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(
                facebook.getPost(adPostId)
            )
        }

        // shared ad posts using workaround
        // TODO check how to get shared posts of ads
//        if (facebookProperties.getProperty("workaround-enabled") == "true" &&
//            facebookSharedPosts !== null) {
//            facebookSharedPosts.openSharedPosts(adPost)
//        }
    }

    logger.info("added comment to ${facebookReplies.commentedPosts} comments")
}
