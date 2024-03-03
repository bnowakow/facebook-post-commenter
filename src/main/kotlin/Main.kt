import facebook4j.*
import mu.KotlinLogging
import kotlin.system.exitProcess


fun main() {

    val logger = KotlinLogging.logger {}
    val facebook: Facebook = FacebookFactory().instance
    val facebook4jProperties = Facebook4jProperties()
    val restfbClient = com.restfb.DefaultFacebookClient(facebook4jProperties.getProperty("oauth.accessToken"),
        facebook4jProperties.getProperty("oauth.appSecret"), com.restfb.Version.VERSION_19_0)
    val facebookReplies = FacebookReplies(facebook, restfbClient)

    val facebookProperties = FacebookProperties()
    var facebookSharedPosts: FacebookSharedPosts? = null
    if (facebookProperties.getProperty("workaround-enabled") == "true") {
        facebookSharedPosts = FacebookSharedPosts()
        facebookSharedPosts.loginToFacebook()
        facebookSharedPosts.inviteToLikeFanpagePeopleWhoInteractedWithPosts()
        facebookSharedPosts.switchProfileToFanPage()
    }

    val fpPostsProcessor = FpPostsProcessor(logger, facebookProperties, facebook4jProperties, facebookReplies, restfbClient, facebookSharedPosts)
    val adPostsProcessor = AdPostsProcessor(logger, facebook, facebookProperties, facebook4jProperties, facebookReplies, facebookSharedPosts)

    // TODO automate inviting people liking page to join grou
    // TODO find notification with people sharing fanpage (orange flag icon in notification) and comment it

    facebook.extendTokenExpiration()

    adPostsProcessor.processAdPost()
    fpPostsProcessor.processFpPost()
    facebookReplies.processRetries()
    fpPostsProcessor.printCommentsSummary()
    exitProcess(0)
}
