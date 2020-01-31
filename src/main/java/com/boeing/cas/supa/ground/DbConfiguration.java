package com.boeing.cas.supa.ground;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.boeing.cas.supa.ground.pojos.KeyVaultProperties;
import com.boeing.cas.supa.ground.utils.KeyVaultRetriever;

@Configuration
@EnableTransactionManagement
public class DbConfiguration {
	
	@Autowired
	private KeyVaultProperties keyVaultProperties;
	
	@Value("${hibernate.dialect}")
    private String hibernateDialect;

    @Value("${hibernate.show_sql}")
    private String hibernateShowSql;

    @Value("${hibernate.hbm2ddl.auto}")
    private String hibernateHbm2ddlAuto;
    
    /*
    @Bean
	public DataSource dataSource(KeyVaultRetriever keyVaultRetriever) {

		DataSourceBuilder factory = DataSourceBuilder.create()
				.driverClassName(keyVaultRetriever.getSecretByKey("SQLDriverClassName"))
				.type(BasicDataSource.class)
				.url(keyVaultRetriever.getSecretByKey("SQLDatabaseUrl"))
				.username(keyVaultRetriever.getSecretByKey("SQLDatabaseUsername"))
				.password(keyVaultRetriever.getSecretByKey("SQLDatabasePassword"));

		return factory.build();
	}
    */	
    
	@Bean
	public DataSource dataSource() {
		DataSourceBuilder builder = DataSourceBuilder.create()
				//.type(BasicDataSource.class)
				.url("jdbc:sqlserver://localhost:1433;database=FDAGroundServices")
				.username("sa")
				.password("MyPa$$w0rd168914");
		
		return builder.build();
	}
	
	@Bean
	public LocalSessionFactoryBean sessionFactory() {
		LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
		sessionFactory.setDataSource(dataSource());
		sessionFactory.setPackagesToScan(this.getClass().getPackage().getName() + ".pojos");
		
		Properties hibernateProperties = new Properties();
		hibernateProperties.put("hibernate.dialect", hibernateDialect);
		hibernateProperties.put("hibernate.show_sql", hibernateShowSql);
		hibernateProperties.put("hibernate.hbmddl.auto", hibernateHbm2ddlAuto);
		hibernateProperties.put("hibernate.enable_lazy_load_no_trans", "true");
		
		sessionFactory.setHibernateProperties(hibernateProperties);
		return sessionFactory;
	}
	
	@Bean
	public HibernateTransactionManager transactionManager() {
		HibernateTransactionManager transactionManager = new HibernateTransactionManager();
		transactionManager.setSessionFactory(sessionFactory().getObject());
		
		return transactionManager;
	}
}