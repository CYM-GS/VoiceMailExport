package de.cymos.voicemailexport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class VoiceMailExport {

    public static final Calendar calendar = Calendar.getInstance();

    private static final Timer timer = new Timer();
    private static final String LAST_EXPORT_FILE = "last_export_date.txt";
    private static final Logger logger = LogManager.getLogger(VoiceMailExport.class);


    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    public static final SimpleDateFormat dateFormatTimeShort = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public static void main(String[] args) {
        if (checkArguments(args)) {
            Console console = System.console();
            if (console == null) {
                logger.error("Access to console was denied.");
            } else {

                System.out.print("IMAP Server: ");
                String imapServer = console.readLine();

                System.out.print("Benutzername: ");
                String username = console.readLine();

                System.out.print("Password: ");
                String password = String.valueOf(console.readPassword());


                // Create Mailer instance
                Mailer mailer = new Mailer(imapServer, username, password, args[0]);


                calendar.add(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);

                logger.info("Waiting for {} till next export.", dateFormatTimeShort.format(calendar.getTime()));

                // Timer task that checks if a new day has started
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (isNewDay()) {
                            if (mailer.getMails("noreply@3cx.net", "Neue Sprachnachricht")) {
                                updateLastExportDate();
                            }
                        }
                    }
                }, 0, 15000);
            }
        } else {
            System.exit(0);
        }
    }

    /**
     * Checks if the current date is different from the last export date.
     * @return true if it's a new day, false if the export already happened today.
     */
    private static boolean isNewDay() {
        String lastExportDate = readLastExportDate();
        String currentDate = getCurrentDate();

        // If the dates are different, it's a new day
        return !currentDate.equals(lastExportDate) && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 0;
    }

    /**
     * Updates the last export date to the current date.
     */
    private static void updateLastExportDate() {
        String currentDate = getCurrentDate();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LAST_EXPORT_FILE))) {
            writer.write(currentDate);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Reads the last export date from the file.
     * @return the last export date as a string, or empty if the file doesn't exist.
     */
    private static String readLastExportDate() {
        try (BufferedReader reader = new BufferedReader(new FileReader(LAST_EXPORT_FILE))) {
            return reader.readLine();
        } catch (IOException e) {
            // If the file doesn't exist or is empty, return an empty string
            return "";
        }
    }

    /**
     * Gets the current date in YYYY-MM-DD format.
     * @return the current date as a string.
     */
    private static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    private static boolean checkArguments(String[] args) {
        if (args.length == 0) {
            logger.error("The save location was not passed!\nExample: java -jar VoiceMailExport-x.x.x.jar C:/Location");
            return false;
        }

        File directory = new File(args[0]);

        if (directory.exists()) {
            if (!directory.isDirectory()) {
                logger.error("The save location must be a directory!");
                return false;
            }

            if (!directory.canWrite()) {
                logger.error("The application has no write access to the set save location!");
                return false;
            }

            return true;
        } else {
            // Try to create the directory
            if (directory.mkdirs()) {
                logger.info("Save location was created successfully: {}", args[0]);
                return true;
            } else {
                logger.info("Error while creating the save location: {}", args[0]);
                return false;
            }
        }
    }

}
