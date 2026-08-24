package kr.or.arte.seal.recipient.importing;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
@ConfigurationProperties("seal.clamav")
public record ClamAvProperties(boolean enabled,String host,int port,Duration timeout) {}
