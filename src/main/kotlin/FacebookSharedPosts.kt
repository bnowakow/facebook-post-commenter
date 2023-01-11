import mu.KotlinLogging
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

class FacebookSharedPosts {

    val driver: WebDriver = ChromeDriver()
    private val logger = KotlinLogging.logger {}
    fun loginToFacebook() {

        val facebookProperties: FacebookProperties = FacebookProperties()

//        System.setProperty("webdriver.chrome.driver", "Yourpath\\chromedriver.exe")

        driver.manage().window().maximize()
        driver["https://www.facebook.com"]
        driver.findElement(By.id("email")).sendKeys(facebookProperties.getProperty("username"))
        driver.findElement(By.id("pass")).sendKeys(facebookProperties.getProperty("password"))
        Thread.sleep(500)
        // cookie form
        driver.findElement(By.className("_9xo6")).click()
        // login button
        driver.findElement(By.name("login")).click()

//        Thread.sleep(7000)
//        driver.findElement(By.className("_4jy0")).click()
        Thread.sleep(5000)
    }

    fun openSharedPosts(postId: String) {

        val id = postId.substringAfter("_")

        // TODO fix notification popup from chrome
        // account icon
        driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[1]/span/div/div[1]")).click()
        Thread.sleep(500)
        // switch profile to fan page
        driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[2]/div/div[2]/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div")).click()
        Thread.sleep(2000)
        driver["https://www.facebook.com/shares/view?id=$id"]
        Thread.sleep(5000)

        // TODO scroll here to bottom of page to load all posts

        var pageSource: String = driver.pageSource.substring(0, driver.pageSource.length)
            .removeRange(0, driver.pageSource.indexOf("Shared with Public</title>"))

        var commentNumber = 1
        while (true) {
//            val firstCommentLinkPosition: Int = pageSource.indexOf("Show Attachment")
//            val firstCommentLinkPosition: Int = pageSource.indexOf("Shared with Public")
            // TODO fix edge case for last comment
//            val secondCommentLinkPosition: Int = pageSource.indexOf("profile.php?id=", firstCommentLinkPosition + 1)
            val secondCommentLinkPosition: Int = pageSource.indexOf("Shared with Public</title>", 1)
            // TODO check witch value is returned when substring is not found
            if (secondCommentLinkPosition == -1) {
                break
            }
            //pageSource.substringAfter("permalink.php?story_fbid=").substringBefore("&") // id of post, wont' use it since API permission is needed to acces post made by others
//            var commentSource: String = pageSource.substring(firstCommentLinkPosition, secondCommentLinkPosition)
            var commentSource: String = pageSource.substring(0, secondCommentLinkPosition)

            var commentTextBoxPosition: Int = commentSource.indexOf("Write a comment")
            if (commentTextBoxPosition > -1) {
                // can be commented
                val adminUsernamePosition = commentSource.indexOf("Kuba Dobrowolski-Nowakowski")
                if (adminUsernamePosition == -1) {
                    // no comment from admin of fanpage
                    val replyMessage: String = FacebookReplies.Companion.randomizeThankYouReply()
                    logger.info("\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")

                    driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                        .sendKeys(replyMessage.replace("\n", Keys.chord(Keys.SHIFT, Keys.ENTER)))
                    Thread.sleep(2000)
                    driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                        .sendKeys(Keys.RETURN)

                    val numberOfSeconds: Long = (10..120).random().toLong()
                    logger.info("\t\t\tsleeping for ${numberOfSeconds} seconds\n")
                    Thread.sleep(1000 * numberOfSeconds)
                }
            }

            commentNumber++
            pageSource = pageSource.removeRange(0, secondCommentLinkPosition)
        }

    }
}