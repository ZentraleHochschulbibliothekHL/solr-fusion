package org.outermedia.solrfusion.query.parser;

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

import lombok.ToString;
import org.outermedia.solrfusion.query.QueryVisitor;
import org.outermedia.solrfusion.types.ScriptEnv;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A date range query.
 *
 * Created by ballmann on 6/27/14.
 */
@ToString(callSuper = true)
public class DateRangeQuery extends NumericRangeQuery<Calendar>
{
    // see lucene's DateTools, but we omit HH (no hours)
    protected static SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    public DateRangeQuery(String field, Calendar min, Calendar max, boolean minInclusive, boolean maxInclusive)
    {
        super(field, minInclusive, maxInclusive, min, max);
    }

    protected DateRangeQuery()
    {
    }

    @Override
    public void accept(QueryVisitor visitor, ScriptEnv env)
    {
        visitor.visitQuery(this, env);
    }

    @Override protected String limitValueAsString(Calendar v)
    {
        String result = "*";
        if(v != null)
        {
            // see lucene's DateTools
            format.setTimeZone(v.getTimeZone());
            result = format.format(v.getTime());
        }
        return result;
    }

    @Override
    public DateRangeQuery shallowClone()
    {
        return shallowCloneImpl(new DateRangeQuery());
    }
}
