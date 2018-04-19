package xdean.spring.message.nest;

import lombok.Data;

@Data
public class NestMessageSourceProperties {
  String prefix = "$(";
  String suffix = ")";
  String splitor = ",";
  String argPrefix = "$";
  String escaper = "\\";
  String quoter = "\"";
}
