package com.mrkirby153.kirbot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.util.concurrent.Executor


/**
 * Configuration for the actual spring context
 */
@Configuration
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
@EnableJpaAuditing
class SpringConfiguration : AsyncConfigurer {
    private val threadPoolTaskExecutor = ThreadPoolTaskExecutor()

    init {
        threadPoolTaskExecutor.corePoolSize = 10
        threadPoolTaskExecutor.maxPoolSize = 10
    }

    override fun getAsyncExecutor(): Executor? {
        threadPoolTaskExecutor.initialize()
        return threadPoolTaskExecutor
    }
}