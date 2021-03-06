package org.outermedia.solrfusion.response.freemarker;

/*
 * #%L
 * SolrFusion
 * %%
 * Copyright (C) 2014 outermedia GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import lombok.Getter;
import org.outermedia.solrfusion.mapper.Term;
import org.outermedia.solrfusion.response.parser.SolrField;

import java.util.List;

/**
 * Data holder class to represent a multivalued or singlevalued field in the freemarker template.
 *
 * @author stephan
 */
public class FreemarkerSingleValuedField
{

    @Getter
    private String name;

    @Getter
    private String type;

    @Getter
    private String value;

    public static FreemarkerSingleValuedField fromSolrField(SolrField sf)
    {
        FreemarkerSingleValuedField freemarkerField = null;
        Term t = sf.getTerm();

        String v = null;
        List<String> fusionFieldValues = t.getFusionFieldValue();

        if (t.isWasMapped() && !t.isRemoved())
        {
            if (fusionFieldValues != null && !fusionFieldValues.isEmpty())
            {
                v = fusionFieldValues.get(0);
            }
            freemarkerField = new FreemarkerSingleValuedField(t.getFusionFieldName(), t.getFusionField().getType(), v);
        }
        return freemarkerField;
    }

    private FreemarkerSingleValuedField(String name, String type, String value)
    {
        this.name = name;
        this.type = type;
        this.value = value;
    }
}