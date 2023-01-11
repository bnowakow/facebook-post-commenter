import mu.KotlinLogging
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.interactions.Actions


class FacebookSharedPosts {

//    val driver: WebDriver = ChromeDriver()
    val driver: WebDriver = FirefoxDriver()
//    val driver: WebDriver = SafariDriver()

    private val logger = KotlinLogging.logger {}
    fun loginToFacebook() {

        val facebookProperties: FacebookProperties = FacebookProperties()

//        System.setProperty("webdriver.chrome.driver", "Yourpath\\chromedriver.exe")

//        driver.manage().window().maximize()
        driver["https://www.facebook.com"]
        driver.findElement(By.id("email")).sendKeys(facebookProperties.getProperty("username"))
        driver.findElement(By.id("pass")).sendKeys(facebookProperties.getProperty("password"))
        Thread.sleep(500)
        // cookie form
        driver.findElement(By.className("_9xo6")).click()
        // login button
        driver.findElement(By.name("login")).click()
        Thread.sleep(5000)
    }

    fun switchProfileToFanPage() {
        // TODO fix notification popup from chrome
        // account icon
        driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[1]/span/div/div[1]")).click()
        Thread.sleep(500)
        // switch profile to fan page
        driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[2]/div/div[2]/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div")).click()
        Thread.sleep(2000)
    }

    fun openSharedPosts(postId: String) {

        val id = postId.substringAfter("_")
        driver["https://www.facebook.com/shares/view?id=$id"]
        Thread.sleep(5000)

        // scroll down to bottom of page to load all posts (lazy loading)
        val js = driver as JavascriptExecutor
        // TODO figure out how to verify if all posts have been loaded
        for (i in 1..500) {
            //Scroll down till the bottom of the page
            js.executeScript("window.scrollBy(0,document.body.scrollHeight)")
            Thread.sleep(600)
            if (i % 50 == 0 || i == 0) {
                // should be debug but can't set netty to info then
                logger.info("\tscrolling for ${i}th time")
            }
        }

        js.executeScript("window.scrollTo(0, -document.body.scrollHeight)")
        Thread.sleep(1000)

        Actions(driver).moveToElement(driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]"))).perform()

        var pageSource: String = driver.pageSource.removeRange(0, driver.pageSource.indexOf("Shared with Public</title>"))

        var commentNumber = 1
        while (true) {
            // TODO fix edge case for last comment
            val secondCommentLinkPosition: Int = pageSource.indexOf("Shared with Public</title>", 1)
            if (secondCommentLinkPosition == -1) {
                break
            }
            //pageSource.substringAfter("permalink.php?story_fbid=").substringBefore("&") // id of post, wont' use it since API permission is needed to acces post made by others
            var commentSource: String = pageSource.substring(0, secondCommentLinkPosition)

            var commentTextBoxPosition: Int = commentSource.indexOf("Write a comment")
            if (commentTextBoxPosition > -1) {
                // can be commented
                logger.debug("\t\tpost ${commentNumber} can be commented")
                val adminUsernamePosition = commentSource.indexOf("Kuba Dobrowolski-Nowakowski")
                if (adminUsernamePosition == -1) {
                    // no comment from admin of fanpage
                    logger.debug("\t\t\tpost ${commentNumber} doesn't contain admin response")
                    val replyMessage: String = FacebookReplies.Companion.randomizeThankYouReply()
                    logger.info("\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")

                    // chrome
//                    driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
//                        .sendKeys(replyMessage.replace("\n", Keys.chord(Keys.SHIFT, Keys.ENTER)))
                    // firefox
                    for (letter in replyMessage.replace("\n", " ")) {
                        driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                            .sendKeys(letter.toString())
                        Thread.sleep(50)
                    }
                    Thread.sleep(500)

                    driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                        .sendKeys(Keys.RETURN)

                    val numberOfSeconds: Long = (10..120).random().toLong()
                    logger.info("\t\t\tsleeping for ${numberOfSeconds} seconds\n")
                    Thread.sleep(1000 * numberOfSeconds)
                } else {
                    logger.debug("\t\t\tpost ${commentNumber} contains admin response")
                    for (i in 1..20) {
                        driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                        Thread.sleep(5)
                    }
                }
            } else {
                logger.debug("\t\tpost ${commentNumber} can't be commented")
                for (i in 1..8) {
                    driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                    Thread.sleep(5)
                }
            }

            commentNumber++
            pageSource = pageSource.removeRange(0, secondCommentLinkPosition)
        }

    }
}