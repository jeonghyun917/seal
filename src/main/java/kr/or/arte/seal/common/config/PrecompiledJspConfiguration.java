package kr.or.arte.seal.common.config;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Element;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods=false)
@ConditionalOnResource(resources="classpath:META-INF/jspc-web.xml")
public class PrecompiledJspConfiguration implements ServletContextInitializer {
    // Embedded Boot does not install WEB-INF/web.xml mappings. Register the
    // generated exact JSP paths explicitly for executable and external WARs.
    @Override public void onStartup(ServletContext context) throws ServletException {
        try (var input = new ClassPathResource("META-INF/jspc-web.xml").getInputStream()) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var document = factory.newDocumentBuilder().parse(input);
            Map<String, ServletRegistration.Dynamic> registrations = new HashMap<>();
            var servlets = document.getElementsByTagName("servlet");
            for (int i = 0; i < servlets.getLength(); i++) {
                var servlet = (Element) servlets.item(i);
                String name = text(servlet, "servlet-name");
                String className = text(servlet, "servlet-class");
                if (!className.startsWith("org.apache.jsp.")) {
                    throw new ServletException("Invalid precompiled JSP class: " + className);
                }
                var registration = context.addServlet(name, className);
                if (registration == null) throw new ServletException("Duplicate JSP servlet: " + name);
                registrations.put(name, registration);
            }
            var mappings = document.getElementsByTagName("servlet-mapping");
            for (int i = 0; i < mappings.getLength(); i++) {
                var mapping = (Element) mappings.item(i);
                String path = text(mapping, "url-pattern");
                if (!path.startsWith("/WEB-INF/") || !path.endsWith(".jsp") || path.contains("*")) {
                    throw new ServletException("Invalid internal JSP mapping: " + path);
                }
                var registration = registrations.get(text(mapping, "servlet-name"));
                if (registration == null || !registration.addMapping(path).isEmpty()) {
                    throw new ServletException("Unable to map precompiled JSP: " + path);
                }
            }
            context.log("Registered " + registrations.size() + " build-time compiled JSP servlets");
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException("Unable to register build-time compiled JSPs", e);
        }
    }

    private static String text(Element element, String tag) {
        return element.getElementsByTagName(tag).item(0).getTextContent().trim();
    }
}
