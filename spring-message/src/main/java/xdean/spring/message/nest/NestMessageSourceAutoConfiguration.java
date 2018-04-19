package xdean.spring.message.nest;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import xdean.spring.message.nest.NestMessageSourceAutoConfiguration.ResourceBundleCondition;

@Configuration
@ConditionalOnMissingBean(value = MessageSource.class, search = SearchStrategy.CURRENT)
@AutoConfigureBefore(MessageSourceAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Conditional(ResourceBundleCondition.class)
@EnableConfigurationProperties
public class NestMessageSourceAutoConfiguration extends MessageSourceAutoConfiguration {
  @Bean
  @ConfigurationProperties(prefix = "xdean.message.nest")
  public NestMessageSourceProperties nestMessageSourceProperties() {
    return new NestMessageSourceProperties();
  }

  @Bean
  @Override
  public MessageSource messageSource() {
    return new NestMessageSource(super.messageSource());
  }

  protected class ResourceBundleCondition extends MessageSourceAutoConfiguration.ResourceBundleCondition {
  }
}
