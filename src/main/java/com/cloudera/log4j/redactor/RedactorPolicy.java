/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.log4j.redactor;

import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;

import java.io.IOException;

/**
 * <code>RewritePolicy</code> implementation that applies the redaction
 * rules defined in the configuration of the <code>RedactorPolicy</code> in
 * the Log4j Properties configuration file.
 *
 * @see RedactorAppender for the redaction rules definition and syntax.
 */
public class RedactorPolicy implements RewritePolicy, OptionHandler {

  // 'rules' is really the name of the file containing the rules
  private String rules;
  private StringRedactor redactor;

  /**
   * Log4j configurator calls this method with the value found in the
   * config file.
   */
  public void setRules(String rules) {
    this.rules = rules;
  }

  /**
   * Called after all options are read in (in our case this is only setRules())
   * so that they can be acted on at one time.  The rules are a full path to
   * a file containing rules in JSON format.  This implements the
   * OptionHandler interface.
   */
  @Override
  public void activateOptions() {
    try {
      redactor = StringRedactor.createFromJsonFile(rules);
    } catch (IOException e) {
      // Changing the exception, since activateOptions can't throw an IOException
      throw new IllegalArgumentException("Problem with rules file " + rules, e);
    }
  }

  /**
   * Given a LoggingEvent, potentially modify it and return an altered copy.
   * This implements the RewritePolicy interface.
   * @param source LoggingEvent to examine
   * @return Either the original (no changes) or a redacted copy.
   */
  @Override
  public LoggingEvent rewrite(LoggingEvent source) {
    String original = source.getMessage().toString();
    String redacted = redactor.redact(original);
    if (!redacted.equals(original)) {
      Throwable throwable = (source.getThrowableInformation() != null)
                            ? source.getThrowableInformation().getThrowable()
                            : null;
      source = new LoggingEvent(source.getFQNOfLoggerClass(),
          source.getLogger(), source.getTimeStamp(), source.getLevel(), redacted,
          throwable);
    }
    return source;
  }
}
