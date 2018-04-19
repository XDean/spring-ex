package xdean.wechat.common.spring.message;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.function.Supplier;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

public class LocaledMessageSource {
  private final Supplier<MessageSource> source;
  private final Supplier<Locale> locale;

  public LocaledMessageSource(Supplier<MessageSource> source, Supplier<Locale> locale) {
    this.source = source;
    this.locale = locale;
  }

  public String getMessage(String code, Object... args) throws NoSuchMessageException {
    return source.get().getMessage(code, args, locale.get());
  }

  public String getMessageDefault(String defaultMessage, String code, Object... args) {
    return source.get().getMessage(code, args, defaultMessage, locale.get());
  }

  public MessageFormat getMessageFormat(String code) {
    return new MessageFormat(getMessage(code), locale.get());
  }

  public MessageSource getSource() {
    return source.get();
  }

  public Locale getLocale() {
    return locale.get();
  }
}
