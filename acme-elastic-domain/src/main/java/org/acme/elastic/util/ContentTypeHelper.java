package org.acme.elastic.util;

import java.net.URLConnection;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Resolves a stored content type from multipart uploads when clients omit or
 * genericize the type.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentTypeHelper {

    /**
     * Multipart clients often send {@code application/octet-stream} or omit the
     * part Content-Type. Prefer the declared type when it is specific; otherwise
     * guess from the original filename (see
     * {@link URLConnection#guessContentTypeFromName}).
     */
    public static String resolveContentType(MultipartFile file) {
        String declared = file.getContentType();
        if (StringUtils.hasText(declared)
                && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(declared)) {
            return declared;
        }
        String name = file.getOriginalFilename();
        if (StringUtils.hasText(name)) {
            String guessed = URLConnection.guessContentTypeFromName(name);
            if (StringUtils.hasText(guessed)) {
                return guessed;
            }
        }
        return StringUtils.hasText(declared) ? declared : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
