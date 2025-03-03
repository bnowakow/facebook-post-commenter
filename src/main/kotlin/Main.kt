import facebook4j.*
import mu.KotlinLogging
import kotlin.system.exitProcess


fun main() {

    val logger = KotlinLogging.logger {}
    val facebook: Facebook = FacebookFactory().instance
    val facebook4jProperties = Facebook4jProperties()
    val facebookProperties = FacebookProperties()
    val restfbClient = com.restfb.DefaultFacebookClient(facebook4jProperties.getProperty("oauth.accessToken"),
        facebook4jProperties.getProperty("oauth.appSecret"), com.restfb.Version.VERSION_19_0)
    val facebookReplies = FacebookReplies(facebook, restfbClient, facebookProperties, facebook4jProperties)

    var facebookSharedPosts: FacebookSharedPosts? = null

    val fpPostsProcessor = FpPostsProcessor(logger, facebookProperties, facebook4jProperties, facebookReplies, restfbClient, facebookSharedPosts)
    val adPostsProcessor = AdPostsProcessor(logger, facebook, facebookProperties, facebook4jProperties, facebookReplies, restfbClient, facebookSharedPosts)

    if (facebookProperties.getProperty("workaround-enabled") == "true") {
        facebookSharedPosts = FacebookSharedPosts(facebook, adPostsProcessor, facebookProperties, facebook4jProperties)
        facebookSharedPosts.loginToFacebook()
        facebookSharedPosts.inviteToLikeFanpagePeopleWhoInteractedWithPosts()
        if (facebookProperties.getProperty("comment-as-fanpage") == "true") {
            facebookSharedPosts.switchProfileToFanPage()
        }
    }
    // TODO figure out if we can use a single reference that would be updated for circular reference above without workaround below
    fpPostsProcessor.facebookSharedPosts = facebookSharedPosts
    adPostsProcessor.facebookSharedPosts = facebookSharedPosts


    // TODO automate inviting people liking page to join group
    // TODO find notification with people sharing fanpage (orange flag icon in notification) and comment it

    facebook.extendTokenExpiration()

    fpPostsProcessor.processFpPost()
    adPostsProcessor.processAdPost()
    facebookReplies.processRetries()
    if (facebookProperties.getProperty("workaround-enabled") == "true") {
        facebookSharedPosts!!.checkIfNewAdPostHasBeenAdded(fpPostsProcessor.postIds)
    }
    fpPostsProcessor.printCommentsSummary()
    // TODO exit selenium browser?
    exitProcess(0)
}
