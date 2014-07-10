package org.outermedia.solrfusion.response;

import freemarker.template.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.outermedia.solrfusion.adapter.SearchServerResponseInfo;
import org.outermedia.solrfusion.configuration.ResponseRendererFactory;
import org.outermedia.solrfusion.response.freemarker.FreemarkerResponse;
import org.outermedia.solrfusion.response.freemarker.FreemarkerResponseHeader;
import org.outermedia.solrfusion.response.parser.Document;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Transforms search results into an output format using freemarker templates.
 *
 * @author stephan
 */
@Slf4j
public class FreemarkerResponseRenderer implements ResponseRendererIfc
{
    public final String XMLTEMPLATEFILE = "xml.ftl";
    public final String JSONTEMPLATEFILE = "json.ftl";
    public final String PHPTEMPLATEFILE = "php.ftl";

    private final String defaultEncoding = "UTF-8";
    private final String defaultLocale = "de-DE";

    private Configuration freemarkerConfig;

    @Getter @Setter
    private String templateFile;

    @Getter
    private String encoding;

    public void setEncoding(String encoding) {
        if (freemarkerConfig == null) return;
        freemarkerConfig.setDefaultEncoding(encoding);
        this.encoding = encoding;
    }

    @Getter
    private String locale;

    public void setLocale(String locale) {
        if (freemarkerConfig == null) return;
        freemarkerConfig.setLocale(Locale.forLanguageTag(locale));
        this.locale = locale;
    }

    public FreemarkerResponseRenderer() {
        templateFile = XMLTEMPLATEFILE;
        encoding = defaultEncoding;
        locale = defaultLocale;
    }

    @Override
    public String getResponseString(ClosableIterator<Document, SearchServerResponseInfo> docStream, String query,
        String filterQuery)
    {
        // prepare the template input:
        Map<String, Object> input = new HashMap<String, Object>();

        FreemarkerResponse freemarkerResponse = new FreemarkerResponse(docStream);
        FreemarkerResponseHeader freemarkerResponseHeader = new FreemarkerResponseHeader(docStream, query, filterQuery);

        input.put("responseHeader", freemarkerResponseHeader);
        input.put("response", freemarkerResponse);

        // Get the template
        Template template = null;
        try {
            template = freemarkerConfig.getTemplate(templateFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write output to the string
        StringWriter stringWriter = new StringWriter();
        // TODO: error handling
        try
        {
            template.process(input, stringWriter);

        }
        catch (TemplateException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return stringWriter.getBuffer().toString();
    }

    @Override
    public void init(ResponseRendererFactory config)
    {
        initFreemarkerConfiguration();

        // TODO: use settings from fusion-schema configuration to overwrite defaults ?
//        Util xmlUtil = new Util();
//
//        String templateFile = null, encoding = null, locale = null;
//        try
//        {
//            templateFile = xmlUtil.getValueOfXpath("//:template-file/text()", config.getFactoryConfig());
//            encoding = xmlUtil.getValueOfXpath("//:encoding/text()", config.getFactoryConfig());
//            locale = xmlUtil.getValueOfXpath("//:locale/text()", config.getFactoryConfig());
//        }
//        catch (XPathExpressionException e)
//        {
//            log.debug("Caught exception while reading freemarker response renderer configuration.", e);
//        }
//
//        if (templateFile != null)
//        {
//            this.setTemplateFile(templateFile);
//        }
//        if (encoding != null)
//        {
//            this.setEncoding(encoding);
//        }
//        if (locale!= null)
//        {
//            this.setLocale(locale);
//        }
    }

    private void initFreemarkerConfiguration()
    {
        // 1. Configure FreeMarker
        freemarkerConfig = new Configuration();
        freemarkerConfig.setWhitespaceStripping(true);

        // Where do we load the templates from:
        freemarkerConfig.setClassForTemplateLoading(FreemarkerResponseRenderer.class, "templates");

        // Some other recommended settings:
        freemarkerConfig.setIncompatibleImprovements(new Version(2, 3, 20));
        freemarkerConfig.setDefaultEncoding(defaultEncoding);
        freemarkerConfig.setLocale(Locale.forLanguageTag(defaultLocale));
        freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

    }

    public static FreemarkerResponseRenderer getInstance()
    {
        return new FreemarkerResponseRenderer();
    }
}

