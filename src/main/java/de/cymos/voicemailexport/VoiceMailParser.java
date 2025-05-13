package de.cymos.voicemailexport;

import javax.mail.*;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceMailParser {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("EEEE, d. MMMM yyyy HH:mm:ss", Locale.GERMAN);

    public static VoiceMail parseFromMailContent(Message message) throws ParseException, MessagingException {
        String[] lines = getContent(message).split("\\r?\\n");
        String caller = null;
        Date callDate = null;
        StringBuilder transcriptBuilder = new StringBuilder();

        boolean readingTranscript = false;

        for (String line : lines) {
            line = line.trim();

            if (readingTranscript) {
                transcriptBuilder.append(line).append(" ");
                continue;
            }

            if (line.startsWith("Von:")) {
                caller = line.substring(4).trim();
            } else if (line.startsWith("Empfangen:")) {
                String dateString = line.substring("Empfangen:".length()).trim().replace("\"", "");
                callDate = DATE_FORMAT.parse(dateString);
            } else if (line.startsWith("Transkription:")) {
                readingTranscript = true;
                transcriptBuilder.append(line.substring("Transkription:".length()).trim()).append(" ");
            }
        }

        return new VoiceMail(
                message.getSentDate(),
                transcriptBuilder.toString().trim(),
                caller,
                callDate
        );
    }

    private static String getContent(Message message) {
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                return (String) content;
            } else if (content instanceof Multipart) {
                return extractTextFromMultipart((Multipart) content);
            }
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to read message content", e);
        }
        return "";
    }

    private static String extractTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                continue; // skip attachments
            }

            Object partContent = part.getContent();
            if (partContent instanceof String) {
                if (part.isMimeType("text/html")) {
                    return (String) partContent; // prefer HTML
                } else if (part.isMimeType("text/plain")) {
                    result.append((String) partContent).append("\n");
                }
            } else if (partContent instanceof Multipart) {
                result.append(extractTextFromMultipart((Multipart) partContent));
            }
        }

        return result.toString().trim();
    }
}
