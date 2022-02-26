package top.shen.ssqmq_server.config;


import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "top.shen.ssqmq_server.admin.mapper",sqlSessionFactoryRef = "adminSqlSessionFactory")
public class DataSourceConfiguration {
	
	@Value("${ssqmq.mybatis.xml.admin}")
	private String adminXml;

	@Bean
	@ConfigurationProperties("ssqmq.datasource.admin")
	public DataSource adminDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean
	public SqlSessionFactory adminSqlSessionFactory(@Qualifier("adminDataSource") DataSource adminDataSource) throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(adminDataSource);
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		sqlSessionFactoryBean.setMapperLocations(resolver.getResources(adminXml));
		return sqlSessionFactoryBean.getObject();
	}
}
