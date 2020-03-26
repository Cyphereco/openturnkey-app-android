package com.cyphereco.openturnkey.utils;

import com.cyphereco.openturnkey.core.Configurations;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

public class Log4jHelper {

    static {
        // For BlockCypher API logger
        org.apache.log4j.BasicConfigurator.configure();
        final org.apache.log4j.Logger l = org.apache.log4j.Logger.getRootLogger();
        l.setLevel(Level.OFF);

        if (!Configurations.writeLogToFile) {
            // Overwrite settings from logback.xml
            configureLogbackDirectly();
        }
    }

    private static void configureLogbackDirectly() {
        // reset the default context (which may already have been initialized)
        // since we want to reconfigure it
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        lc.stop();

        // setup LogcatAppender
        PatternLayoutEncoder encoder2 = new PatternLayoutEncoder();
        encoder2.setContext(lc);
        encoder2.setPattern("%msg%n");
        encoder2.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(lc);
        logcatAppender.setEncoder(encoder2);
        logcatAppender.start();

        // add the newly created appenders to the root logger;
        // qualify Logger to disambiguate from org.slf4j.Logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);//root.addAppender(fileAppender);
        root.addAppender(logcatAppender);
    }

    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
}
