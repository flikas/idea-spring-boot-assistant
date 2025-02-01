package dev.flikas.spring.boot.assistant.idea.plugin.report;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.stream.Stream;

public class GitHubReporter extends ErrorReportSubmitter {
  @Override
  public @NlsActions.ActionText @NotNull String getReportActionText() {
    return "Create Bug Report at GitHub";
  }


  @Override
  public boolean submit(
      @NotNull IdeaLoggingEvent[] events, @Nullable String additionalInfo,
      @NotNull Component parentComponent, @NotNull Consumer<? super SubmittedReportInfo> consumer
  ) {
    if (events.length == 0) {
      return false;
    }

    final StringBuilder body = new StringBuilder();

    body.append("## What happened\n");
    if (additionalInfo != null) {
      body.append(additionalInfo.trim()).append("\n");
    } else {
      body.append("> Please describe what you were doing when this exception occurred.\n");
    }
    body.append("\n");

    body.append("## Expected behavior\n")
        .append("> A clear and concise description of what you expected to happen.\n")
        .append("\n");

    body.append("## Screenshots\n")
        .append("> If applicable, add screenshots to help explain your problem.\n")
        .append("\n");

    OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    String osInfo = String.format("%s %s %s", os.getName(), os.getVersion(), os.getArch());
    body.append("## Version information\n")
        .append("- OS: `").append(osInfo).append("`\n");
    // IntelliJ version
    final ApplicationInfo info = ApplicationInfo.getInstance();
    body.append("- IDE: `")
        .append(info.getVersionName()).append(' ').append(info.getFullVersion())
        .append('(').append(info.getBuild()).append(')')
        .append("`\n");
    // Plugin version
    PluginId pid = PluginId.getId("dev.flikas.idea.spring.boot.assistant.plugin");
    final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(pid);
    assert plugin != null;
    body.append("- Plugin: `")
        .append(plugin.getName()).append(' ').append(plugin.getVersion()).append("`\n")
        .append("\n");

    for (IdeaLoggingEvent event : events) {
      body.append("## Exception\n")
          .append(event.getMessage()).append("\n")
          .append("\n");
      if (event.getThrowable() != null) {
        body.append("<details><summary>Stack trace</summary>\n\n")
            .append("```\n")
            .append(event.getThrowableText().trim()).append("\n")
            .append("```\n")
            .append("</details>\n\n");
      }
    }

    body.append("## Additional context\n")
        .append("> Add any other context about the problem here.\n");

    try {
      URIBuilder uriBuilder = new URIBuilder("https://github.com/flikas/idea-spring-boot-assistant/issues/new");
      uriBuilder.addParameter("body", body.toString());
      uriBuilder.addParameter("title", "[BUG]" + StringUtil.capitalize(Stream
          .of(events)
          .map(IdeaLoggingEvent::getMessage)
          .filter(Objects::nonNull)
          .filter(StringUtils::isNotBlank)
          .findAny()
          .orElseGet(() -> Stream
              .of(events)
              .map(IdeaLoggingEvent::getThrowableText)
              .filter(Objects::nonNull)
              .map(StringUtil::splitByLines)
              .filter(arr -> arr.length > 0)
              .map(arr -> arr[0])
              .findAny()
              .orElse("")
          )));
      uriBuilder.addParameter("labels", "bug");
      BrowserUtil.browse(uriBuilder.build());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    consumer.consume(new SubmittedReportInfo(
        null,
        "",
        SubmittedReportInfo.SubmissionStatus.NEW_ISSUE
    ));

    return true;
  }

}
