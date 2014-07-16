package org.outermedia.solrfusion.types;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.outermedia.solrfusion.configuration.Util;
import org.w3c.dom.Element;

import java.io.File;
import java.util.List;

/**
 * A Bean Shell interpreter which evaluates expressions contained in a file to process a field conversion.
 *
 * @author ballmann
 */

@ToString(callSuper = true)
@Slf4j
public class BshFile extends Bsh
{

    /**
     * The expected configuration is:
     * <pre>
     * {@code<file>path-to-code.bsh</file>}
     * </pre>
     * @param typeConfig a list of XML elements
     * @param util       helper which simplifies to apply xpaths
     */
    @Override
    public void passArguments(List<Element> typeConfig, Util util)
    {
        try
        {
            String fileName = getConfigString("file", typeConfig, util);
            setCode(FileUtils.readFileToString(new File(fileName)));
        }
        catch (Exception e)
        {
            log.error("Caught exception while parsing configuration: "
                    + elementListToString(typeConfig), e);
        }
        logBadConfiguration(getCode() != null, typeConfig);
    }

    public static BshFile getInstance()
    {
        return new BshFile();
    }
}
