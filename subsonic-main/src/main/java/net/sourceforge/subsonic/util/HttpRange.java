/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class HttpRange {

    private static final Pattern PATTERN = Pattern.compile("bytes=(\\d+)-(\\d*)");
    private final long firstBytePos;
    private final Long lastBytePos;
    private final Long totalLength;

    /**
     * Parses the given string as a HTTP header byte range.  See chapter 14.36.1 in RFC 2068
     * for details.
     * <p/>
     * Only a subset of the allowed syntaxes are supported. Only ranges which specify first-byte-pos
     * are supported. The last-byte-pos is optional.
     *
     * @param range The range from the HTTP header, for instance "bytes=0-499" or "bytes=500-"
     * @param totalLength The total length of the content, or {@code null} if unknown.
     * @return A range object (using inclusive values). If the last-byte-pos is not given, the end of
     *         the returned range is {@code null}. The method returns <code>null</code> if the syntax
     *         of the given range is not supported.
     */
    public static HttpRange of(String range, Long totalLength) {
        if (range == null) {
            return null;
        }

        Matcher matcher = PATTERN.matcher(range);
        if (matcher.matches()) {
            String firstString = matcher.group(1);
            String lastString = StringUtils.trimToNull(matcher.group(2));

            Long first = Long.parseLong(firstString);
            Long last = lastString == null ? null : Long.parseLong(lastString);

            if (last != null && first > last) {
                return null;
            }
            return new HttpRange(first, last, totalLength);
        }
        return null;
    }

    public static HttpRange of(HttpServletRequest request, Long totalLength) {
        return request == null ? null : of(request.getHeader("Range"), totalLength);
    }

    public static HttpRange of(HttpServletRequest request) {
        return of(request, null);
    }

    public HttpRange(long firstBytePos, Long lastBytePos, Long totalLength) {
        this.firstBytePos = firstBytePos;
        this.lastBytePos = lastBytePos;
        this.totalLength = totalLength;
    }

    public long getOffset() {
        return getFirstBytePos();
    }

    public long getLength() {
        if (isClosed()) {
            return size();
        }
        if (totalLength == null) {
            return -1;
        }
        return totalLength - getOffset();
    }

    /**
     * @return The first byte position (inclusive) in the range. Never {@code null}.
     */
    public long getFirstBytePos() {
        return firstBytePos;
    }

    /**
     * @return The last byte position (inclusive) in the range. Can be {@code null}.
     */
    public Long getLastBytePos() {
        return lastBytePos;
    }

    /**
     * @return Whether this is a closed range (both first and last byte position specified).
     */
    public boolean isClosed() {
        return lastBytePos != null;
    }

    /**
     * @return The size in bytes if the range is closed, -1 otherwise.
     */
    public long size() {
        return isClosed() ? (lastBytePos - firstBytePos + 1) : -1;
    }

    /**
     * @return Returns whether the given byte position is within this range.
     */
    public boolean contains(long pos) {
        if (pos < firstBytePos) {
            return false;
        }
        return lastBytePos == null || pos <= lastBytePos;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(firstBytePos).append('-');
        if (lastBytePos != null) {
            builder.append(lastBytePos);
        }
        return builder.toString();
    }
}
