package de.cymos.voicemailexport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import javax.mail.*;
import javax.mail.search.SearchTerm;

public class Mailer {

    private final String path;
    private final Logger logger = LogManager.getLogger(Mailer.class);
    private final String host;
    private final String username;
    private final String password;

    private Store store = null;

    public Mailer(String host, String username, String password, String path) {
        this.path = path;
        this.host = host;
        this.username = username;
        this.password = password;

        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", host);
        props.put("mail.imap.port", "993");
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.protocols", "TLSv1.2 TLSv1.3");

        try {
            Session session = Session.getDefaultInstance(props);
            store = session.getStore("imap");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void ensureConnected() throws MessagingException {
        if (!store.isConnected()) {
            store.connect(host, username, password);
        }
    }

    public boolean getMails(String sender, String subject) {
        Message[] messages;
        try {
            ensureConnected();
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            SearchTerm searchTerm = new SearchTerm() {
                @Override
                public boolean match(Message msg) {
                    try {
                        return (msg.getSubject() != null && msg.getSubject().contains(subject)) &&
                                (Arrays.stream(msg.getFrom()).anyMatch(x -> x.toString().contains(sender)));
                    } catch (MessagingException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            messages = inbox.search(searchTerm);

            if (messages.length == 0) {
                logger.info("No matching emails found.");
                inbox.close(false);
                return true;
            }

            List<VoiceMail> voiceMails = new ArrayList<>();
            for (Message message : messages) {
                voiceMails.add(VoiceMailParser.parseFromMailContent(message));
            }

            VoiceMailExcelExporter.exportToExcel(voiceMails,
                    String.format(path + "/VoiceMail_%s.xlsx", VoiceMailExport.dateFormat.format(new Date())));

            // Prepare target folder path
            String targetFolderName = "INBOX/Mailbox Verarbeitet";

            Folder targetFolder = store.getFolder(targetFolderName);
            if (!targetFolder.exists()) {
                targetFolder.create(Folder.HOLDS_MESSAGES);
            }
            targetFolder.open(Folder.READ_WRITE);

            // Copy messages to the target folder
            inbox.copyMessages(messages, targetFolder);

            // Set the DELETED flag on the original messages
            for (Message msg : messages) {
                msg.setFlag(Flags.Flag.DELETED, true);
            }

            // Expunge the source folder to permanently delete the messages
            inbox.close(true); // true = expunge deleted messages
            targetFolder.close(false);
            store.close();

            logger.info("Processed and moved {} message(s).", messages.length);
            logger.info("Exported collection to save location.");
            VoiceMailExport.calendar.add(Calendar.DAY_OF_MONTH, 1);
            logger.info("Next Export queued for {}", VoiceMailExport.dateFormatTimeShort.format(VoiceMailExport.calendar.getTime()));
            return true;
        } catch (Exception e) {
            logger.error("Error while processing emails: {}", e.getMessage());
            return false;
        }
    }
}