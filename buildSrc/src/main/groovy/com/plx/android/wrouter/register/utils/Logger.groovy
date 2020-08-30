package com.plx.android.wrouter.register.utils;

import org.gradle.api.Project

/**
 * Format log
 */
class Logger {
    static final TAG = 'WRouter::AutoRegister >>> '
    static org.gradle.api.logging.Logger logger

    static void make(Project project) {
        logger = project.getLogger()
    }

    static void i(String info) {
        if (null != info && null != logger) {
            logger.info(TAG + info)
        }
    }

    static void e(String error) {
        if (null != error && null != logger) {
            logger.error(TAG + error)
        }
    }

    static void w(String warning) {
        if (null != warning && null != logger) {
            logger.warn(TAG + warning)
        }
    }
}
