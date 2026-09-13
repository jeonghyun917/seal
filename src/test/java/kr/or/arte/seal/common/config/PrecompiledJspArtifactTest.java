package kr.or.arte.seal.common.config;
import jakarta.servlet.Servlet;
import org.junit.jupiter.api.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PrecompiledJspArtifactTest {
    @Test void everySourceJspHasALoadableCompiledServletAndAnExactMapping() throws Exception {
        var resource=getClass().getResourceAsStream("/META-INF/jspc-web.xml");
        assertThat(resource).as("Maven must compile JSPs before tests/package").isNotNull();
        try(resource) {
            var doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resource);
            Set<String> patterns=new HashSet<>();
            var mappings=doc.getElementsByTagName("url-pattern");
            for(int i=0;i<mappings.getLength();i++)patterns.add(mappings.item(i).getTextContent().trim());
            Path web=Path.of("src/main/webapp");
            try(var files=Files.walk(web)){
                Set<String> expected=new HashSet<>();
                files.filter(p->p.toString().endsWith(".jsp")).forEach(p->expected.add("/"+web.relativize(p).toString().replace('\\','/')));
                assertThat(patterns).containsExactlyInAnyOrderElementsOf(expected);
            }
            var classes=doc.getElementsByTagName("servlet-class");
            assertThat(classes.getLength()).isEqualTo(patterns.size());
            for(int i=0;i<classes.getLength();i++) {
                Class<?> generated=Class.forName(classes.item(i).getTextContent().trim(),false,getClass().getClassLoader());
                assertThat(Servlet.class.isAssignableFrom(generated)).isTrue();
            }
        }
    }
}
