package pl.bnowakowski.facebook_commenter.logging

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.LayoutBase;
import java.sql.Timestamp
import java.text.SimpleDateFormat

class LoggingLayout : LayoutBase<ILoggingEvent>() {

    // https://logback.qos.ch/manual/layouts.html
    override fun doLayout(event: ILoggingEvent?): String {
        val sbuf = StringBuffer(128);
        // https://stackoverflow.com/a/1156482
        // https://www.youtube.com/watch?v=VZ-NCt-Iu5g
        sbuf.append(SimpleDateFormat("HH:mm:ss.SSS").format(Timestamp(event!!.timeStamp)))
        sbuf.append(" ");
        sbuf.append(event.level);
        // to make it same lenght as DEBUG
        if (event.level.toString() == "INFO") {
            sbuf.append(" ")
        }

        // disabled until using threads
        /*
        sbuf.append(" [");
        sbuf.append(event.threadName);
        sbuf.append("] ");
        */

        sbuf.append(event.loggerName)
        // TODO below is stupid and should be based on event.loggerName.length instead
        if (event.loggerName == "FacebookReplies") {
            sbuf.append("\t\t")
        }
        if (event.loggerName == "FacebookSharedPosts") {
            sbuf.append("\t")
        }
        if (event.loggerName == "Main") {
            sbuf.append("\t\t\t\t\t")
        }
        sbuf.append(" - ");
        sbuf.append(event.formattedMessage);
        sbuf.append(CoreConstants.LINE_SEPARATOR);
        return sbuf.toString();
    }

}
