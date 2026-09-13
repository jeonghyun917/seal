package kr.or.arte.seal.common.config;
import jakarta.servlet.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrecompiledJspConfigurationTest {
    @Test void registersEveryGeneratedServletOnItsExactInternalJspPath() throws Exception {
        ServletContext context=mock(ServletContext.class);
        Map<String,String> routes=new HashMap<>();
        when(context.addServlet(anyString(),anyString())).thenAnswer(call->{
            String clazz=call.getArgument(1);
            assertThat(Servlet.class.isAssignableFrom(Class.forName(clazz))).isTrue();
            ServletRegistration.Dynamic registration=mock(ServletRegistration.Dynamic.class);
            when(registration.addMapping(any(String[].class))).thenAnswer(mapping->{
                for(Object arg:mapping.getArguments()){
                    String path=(String)arg;
                    assertThat(path).startsWith("/WEB-INF/").endsWith(".jsp");
                    assertThat(routes.put(path,clazz)).isNull();
                }
                return Set.of();
            });
            return registration;
        });
        new PrecompiledJspConfiguration().onStartup(context);
        try(var input=getClass().getResourceAsStream("/META-INF/jspc-web.xml")){
            var doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
            var paths=doc.getElementsByTagName("url-pattern");
            assertThat(routes).hasSize(paths.getLength());
            for(int i=0;i<paths.getLength();i++)assertThat(routes).containsKey(paths.item(i).getTextContent().trim());
        }
    }
    @Test void refusesConflictingServletRegistrations() {
        ServletContext context=mock(ServletContext.class);
        assertThatThrownBy(()->new PrecompiledJspConfiguration().onStartup(context)).isInstanceOf(ServletException.class);
    }
}
