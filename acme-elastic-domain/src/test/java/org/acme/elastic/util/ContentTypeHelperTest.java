package org.acme.elastic.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLConnection;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ContentTypeHelperTest {

    @Test
    void resolveContentType_specificDeclared_unchanged() {
        MultipartFile file = new MockMultipartFile("file", "ignored.bin", "application/pdf", new byte[0]);
        assertEquals("application/pdf", ContentTypeHelper.resolveContentType(file));
    }

    @Test
    void resolveContentType_textPlainDeclared_unchanged() {
        MultipartFile file = new MockMultipartFile("file", "notes.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        assertEquals(MediaType.TEXT_PLAIN_VALUE, ContentTypeHelper.resolveContentType(file));
    }

    @Test
    void resolveContentType_octetStream_usesFilenameGuess() {
        String filename = "document.json";
        String expected = URLConnection.guessContentTypeFromName(filename);
        MultipartFile file = new MockMultipartFile("file", filename, MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[0]);
        assertEquals(expected, ContentTypeHelper.resolveContentType(file));
    }

    @Test
    void resolveContentType_octetStream_txt_guessesTextPlain() {
        String filename = "readme.txt";
        String expected = URLConnection.guessContentTypeFromName(filename);
        MultipartFile file = new MockMultipartFile("file", filename, MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[0]);
        assertEquals(expected, ContentTypeHelper.resolveContentType(file));
    }

    @Test
    void resolveContentType_noGuess_fallsBackToDeclaredOctetStream() {
        MultipartFile file = new MockMultipartFile("file", "x.unknown-ext-xyz",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, ContentTypeHelper.resolveContentType(file));
    }

    @Test
    void resolveContentType_nullDeclared_guessesFromFilename() {
        String filename = "data.json";
        String expected = URLConnection.guessContentTypeFromName(filename);
        MultipartFile file = new MockMultipartFile("file", filename, null, new byte[0]);
        assertEquals(expected, ContentTypeHelper.resolveContentType(file));
    }

    @Test
    void resolveContentType_nullDeclared_noFilename_defaultsToOctetStream() {
        MultipartFile file = new MockMultipartFile("file", null, null, new byte[0]);
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, ContentTypeHelper.resolveContentType(file));
    }
}
