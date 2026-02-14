package com.github.schaka.naviseerr.config

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.spring.boot.autoconfigure.ExposedAutoConfiguration
import org.jetbrains.exposed.v1.spring.transaction.ExposedSpringTransactionAttributeSource
import org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
@ImportAutoConfiguration(
    exclude = [DataSourceTransactionManagerAutoConfiguration::class, ExposedAutoConfiguration::class]
)
class ExposedConfig {

    @Bean
    fun databaseConfig() = DatabaseConfig {
        useNestedTransactions = true
    }

    @Value($$"${spring.exposed.excluded-packages:}#{T(java.util.Collections).emptyList()}")
    private lateinit var excludedPackages: List<String>

    @Value($$"${spring.exposed.show-sql:false}")
    private var showSql: Boolean = false

    /**
     * Returns a [SpringTransactionManager] instance using the specified [datasource] and [databaseConfig].
     *
     * To enable logging of all transaction queries by the SpringTransactionManager instance, set the property
     * `spring.exposed.show-sql` to `true` in the application.properties file.
     */
    @Bean
    open fun springTransactionManager(datasource: DataSource, databaseConfig: DatabaseConfig): SpringTransactionManager {
        return SpringTransactionManager(datasource, databaseConfig, showSql)
    }

    /**
     * Returns an [ExposedSpringTransactionAttributeSource] instance.
     *
     * To enable rollback when ExposedSQLException is Thrown
     *
     * '@Primary' annotation is used to avoid conflict with default TransactionAttributeSource bean
     * than enable when use '@EnableTransactionManagement'
     */
    @Bean
    @Primary
    open fun exposedSpringTransactionAttributeSource(): ExposedSpringTransactionAttributeSource {
        return ExposedSpringTransactionAttributeSource()
    }
}