package xdean.spring.message.nest;

import static xdean.jex.extra.IllegalDefineException.assertTrue;
import static xdean.jex.extra.IllegalDefineException.assertFalse;
import static xdean.jex.util.task.TaskUtil.firstSuccess;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import lombok.RequiredArgsConstructor;

public class NestMessageSource implements MessageSource {
  private final MessageSource delegate;
  private String prefix = "$(";
  private String suffix = ")";
  private String splitor = ",";
  private String argPrefix = "$";
  private String escaper = "\\";
  private String quoter = "\"";

  public NestMessageSource(MessageSource delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
    return resolve(delegate.getMessage(code, args, defaultMessage, locale), args, locale);
  }

  @Override
  public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
    return resolve(delegate.getMessage(code, args, locale), args, locale);
  }

  @Override
  public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
    return resolve(delegate.getMessage(resolvable, locale), resolvable.getArguments(), locale);
  }

  private String resolve(String str, Object[] args, Locale locale) {
    return new Resolver(str, args, locale).parse();
  }

  @RequiredArgsConstructor
  private class Resolver {
    private final Token prefixToken = new Token(prefix);
    private final Token splitorToken = new Token(splitor);
    private final String str;
    private final Object[] args;
    private final Locale locale;
    private int offset = 0;
    private int left = 0;
    private boolean inQuote = false;
    private boolean inArg = false;
    private Deque<Token> values = new ArrayDeque<>();

    public String parse() {
      values.push(new Token());
      while (offset < str.length()) {
        if (str.startsWith(escaper, offset)) {
          offset(escaper);
          checkOffset("Can't end with escaper");
          append();
        } else if (inQuote) {
          if (str.startsWith(quoter, offset)) {
            offset(quoter);
            inQuote = false;
          } else {
            append();
          }
        } else if (inArg) {
          if (str.startsWith(suffix, offset) || str.startsWith(splitor, offset)) {
            inArg = false;
            Token argToken = values.pop();
            String s = argToken.sb.toString().trim();
            try {
              int index = Integer.parseInt(s);
              assertTrue(args.length > index, formatError("Arguement " + index + " not exist"));
              values.push(new Token(args[index]));
            } catch (NumberFormatException e) {
              assertTrue(false, formatError("ArgPrefix " + argPrefix + " must only follow a integer"));
            }
          } else {
            append();
          }
        } else if (str.startsWith(prefix, offset)) {
          values.push(prefixToken);
          values.push(splitorToken);
          end();
          offset(prefix);
          left++;
        } else if (left > 0) {
          if (str.startsWith(suffix, offset)) {
            offset(suffix);
            left--;
            resolveExp();
            end();
          } else if (str.startsWith(quoter, offset)) {
            offset(quoter);
            inQuote = true;
          } else if (str.startsWith(splitor, offset)) {
            offset(splitor);
            resolveArg();
            values.push(splitorToken);
            end();
          } else if (str.startsWith(argPrefix, offset) && values.peek().toString().isEmpty()) {
            offset(argPrefix);
            inArg = true;
          } else {
            append();
          }
        } else {
          append();
        }
      }
      assertFalse(inQuote, formatError("Quote not closed"));
      assertTrue(left == 0, formatError("Prefix and Suffix not match"));
      return toString(values, false);
    }

    private String toString(Deque<Token> tokens, boolean ascend) {
      return StreamSupport
          .stream(Spliterators.spliterator(ascend ? tokens.iterator() : tokens.descendingIterator(), tokens.size(), 0), false)
          .map(s -> s.toString())
          .collect(Collectors.joining());
    }

    private void resolveArg() {
      Deque<Token> params = new ArrayDeque<>();
      while (true) {
        Token pop = values.pop();
        if (pop == splitorToken) {
          break;
        }
        params.push(pop);
      }
      if (params.size() == 1) {
        Token pop = params.pop();
        if (pop.toString().isEmpty()) {
          values.push(new Token(null));
        } else {
          values.push(pop);
        }
      } else {
        values.push(new Token(toString(params, true)));
      }
    }

    private void resolveExp() {
      resolveArg();
      Deque<Token> params = new ArrayDeque<>();
      while (true) {
        Token pop = values.pop();
        if (pop == prefixToken) {
          break;
        }
        params.push(pop);
      }
      assertFalse(params.isEmpty(), formatError("Can't define empty var " + prefix + suffix));
      Token codeToken = params.pop();
      assertTrue(codeToken.arg == null, formatError("First parameter should be code"));
      String code = codeToken.sb.toString();
      Object[] newArgs = params.stream().map(s -> s.getArg()).toArray();
      String ret = getMessage(code, newArgs, locale);
      values.push(new Token(ret));
    }

    private void end() {
      values.push(new Token());
    }

    private void append() {
      values.peek().append(str.charAt(offset));
      offset(1);
    }

    private void offset(int i) {
      offset += i;
    }

    private void offset(String token) {
      offset(token.length());
    }

    private void checkOffset(String msg) {
      assertTrue(offset < str.length(), formatError(msg));
    }

    private String formatError(String msg) {
      return String.format("%s: %d@%s", msg, offset, str);
    }

    private class Token {
      public final StringBuilder sb = new StringBuilder();
      public final Object arg;
      private final boolean isArg;

      public Token() {
        arg = null;
        isArg = false;
      }

      public Token(Object arg) {
        this.arg = arg;
        isArg = true;
      }

      public void append(char s) {
        assertFalse(isArg, formatError("Can't append to arg"));
        sb.append(s);
      }

      public Object getArg() {
        if (isArg) {
          return arg;
        } else {
          String s = sb.toString();
          return firstSuccess(
              () -> Integer.valueOf(s),
              () -> Double.valueOf(s),
              () -> s);
        }
      }

      @Override
      public String toString() {
        return Objects.toString(getArg());
      }
    }
  }
}
