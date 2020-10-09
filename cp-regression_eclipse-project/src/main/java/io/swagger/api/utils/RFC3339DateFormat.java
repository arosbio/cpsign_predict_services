package io.swagger.api.utils;

import java.text.FieldPosition;
import java.util.Date;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.databind.util.StdDateFormat;

public class RFC3339DateFormat extends ISO8601DateFormat {

	private static final long serialVersionUID = 7638604584299113451L;

	// Same as ISO8601DateFormat but serializing milliseconds.
    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        String value = new StdDateFormat().format(date);
        toAppendTo.append(value);
        return toAppendTo;
    }

}